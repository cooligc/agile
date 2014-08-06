/*
 * HeadsUp Agile
 * Copyright 2009-2014 Heads Up Development Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.headsupdev.agile.app.milestones;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.value.ValueMap;
import org.headsupdev.agile.api.Manager;
import org.headsupdev.agile.api.MenuLink;
import org.headsupdev.agile.api.Permission;
import org.headsupdev.agile.api.User;
import org.headsupdev.agile.app.milestones.permission.MilestoneEditPermission;
import org.headsupdev.agile.app.milestones.permission.MilestoneViewPermission;
import org.headsupdev.agile.storage.Comment;
import org.headsupdev.agile.storage.HibernateStorage;
import org.headsupdev.agile.storage.StoredProject;
import org.headsupdev.agile.storage.dao.MilestonesDAO;
import org.headsupdev.agile.storage.issues.Issue;
import org.headsupdev.agile.storage.issues.Milestone;
import org.headsupdev.agile.web.*;
import org.headsupdev.agile.web.components.FormattedDateModel;
import org.headsupdev.agile.web.components.MarkedUpTextModel;
import org.headsupdev.agile.web.components.issues.IssueFilterPanel;
import org.headsupdev.agile.web.components.issues.IssueListPanel;
import org.headsupdev.agile.web.wicket.SortableEntityProvider;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.*;

/**
 * Milestone view page
 *
 * @author Andrew Williams
 * @version $Id$
 * @since 1.0
 */
@MountPoint("view")
public class ViewMilestone
        extends HeadsUpPage
{
    private MilestonesDAO dao = new MilestonesDAO();

    private Milestone milestone;
    private HeadsUpPage page;
    private IssueFilterPanel filter;

    public Permission getRequiredPermission()
    {
        return new MilestoneViewPermission();
    }

    public void layout()
    {
        super.layout();
        page = this;
        add( CSSPackageResource.getHeaderContribution( getClass(), "milestone.css" ) );

        String name = getPageParameters().getString( "id" );

        milestone = dao.find( name, getProject() );
        if ( milestone == null )
        {
            notFoundError();
            return;
        }

        addLinks( getLinks( milestone ) );
        addDetails();

        User currentUser = getSession().getUser();
        final boolean userHasPermission = Manager.getSecurityInstance().userHasPermission( currentUser, new MilestoneEditPermission(), getProject() );

        final List<Comment> commentList = new LinkedList<Comment>();
        commentList.addAll( milestone.getComments() );
        Collections.sort( commentList, new Comparator<Comment>()
        {
            public int compare( Comment comment1, Comment comment2 )
            {
                return comment1.getCreated().compareTo( comment2.getCreated() );
            }
        } );
        add( new ListView<Comment>( "comments", commentList )
        {

            private Comment comment;

            protected void populateItem( ListItem<Comment> listItem )
            {
                comment = listItem.getModelObject();
                listItem.add( new Image( "icon", new ResourceReference( HeadsUpPage.class, "images/comment.png" ) ) );
                listItem.add( new Label( "username", comment.getUser().getFullnameOrUsername() ) );
                listItem.add( new Label( "created", new FormattedDateModel( comment.getCreated(),
                        ( (HeadsUpSession) getSession() ).getTimeZone() ) ) );

                Link edit = new Link( "editComment" )
                {
                    @Override
                    public void onClick()
                    {
                        getPage().getPageParameters().put( "commentId", comment.getId() );
                        setResponsePage( EditComment.class, getPage().getPageParameters() );
                    }
                };
                listItem.add( edit.setVisible( userHasPermission ) );

                Link remove = new Link( "removeComment" )
                {
                    @Override
                    public void onClick()
                    {
                        comment = (Comment) ( (HibernateStorage) getStorage() ).getHibernateSession().merge( comment );
                        milestone = (Milestone) ( (HibernateStorage) getStorage() ).getHibernateSession().merge( milestone );
                        commentList.remove( comment );

                        Iterator<Comment> iterator = milestone.getComments().iterator();
                        while ( iterator.hasNext() )
                        {
                            Comment current = iterator.next();
                            if ( comment.getId() == current.getId() )
                            {
                                iterator.remove();
                            }
                        }
                        ( (HibernateStorage) getStorage() ).delete( comment );
                        milestone.setUpdated( new Date() );
                    }
                };
                listItem.add( remove.setVisible( userHasPermission ) );

                listItem.add( new Label( "comment", new MarkedUpTextModel( comment.getComment(), getProject() ) )
                        .setEscapeModelStrings( false ) );
            }
        } );

        filter = new IssueFilterPanel( "filter", getSession().getUser() );
        if ( milestone.isCompleted() )
        {
            filter.setStatuses( new boolean[]{false, false, false, false, false, true, true} );
        }
        else
        {
            filter.setStatuses( new boolean[]{true, true, true, true, true, false, false} );
        }
        add( filter );

        add( new IssueListPanel( "issuelist", getIssuesProvider(), this, true, true, milestone ) );

        boolean timeEnabled = Boolean.parseBoolean( milestone.getProject().getConfigurationValue(
                StoredProject.CONFIGURATION_TIMETRACKING_ENABLED ) ) && milestone.hasValidTimePeriod();
        add( new Image( "graph", new ResourceReference( "burndown.png" ), getPageParameters() ).setVisible( timeEnabled ) );
        add( new WorkRemainingTable( "table", milestone ).setVisible( timeEnabled ) );

        ValueMap params = new ValueMap();
        params.put( "project", getProject().getId() );
        params.put( "id", milestone.getName() );
        params.put( "silent", true );
        add( new ResourceLink( "export", new ResourceReference( "export-worked.csv" ), params ).setVisible( timeEnabled ) );
    }

    public Milestone getMilestone()
    {
        return milestone;
    }

    private SortableEntityProvider<Issue> getIssuesProvider()
    {
        return new SortableEntityProvider<Issue>()
        {
            @Override
            protected Criteria createCriteria()
            {
                Session session = ( (HibernateStorage) Manager.getStorageInstance() ).getHibernateSession();

                Criteria c = session.createCriteria( Issue.class );
                c.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );
                c.add( filter.getStatusCriterion() );
                Criterion assignmentRestriction = filter.getAssignmentCriterion();
                if ( assignmentRestriction != null )
                {
                    c.add( assignmentRestriction );
                }

                c.add( Restrictions.eq( "milestone", milestone ) );
                return c;
            }

            @Override
            protected List<Order> getDefaultOrder()
            {
                return Arrays.asList( Order.asc( "priority" ),
                        Order.asc( "rank" ), Order.asc( "status" ), Order.asc( "id.id" ) );
            }
        };
    }

    @Override
    public String getPageTitle()
    {
        return "Milestone:" + milestone.getName() + PAGE_TITLE_SEPARATOR + super.getPageTitle();
    }

    protected void addDetails()
    {
        add( new MilestonePanel( "milestone", milestone ) );
    }

    public static List<MenuLink> getLinks( Milestone milestone )
    {
        List<MenuLink> links = new LinkedList<MenuLink>();
        PageParameters addIssueParams = new PageParameters();
        addIssueParams.add( "project", milestone.getProject().getId() );
        addIssueParams.add( "milestone", milestone.getName() );

        PageParameters pageParams = new PageParameters();
        pageParams.add( "project", milestone.getProject().getId() );
        pageParams.add( "id", milestone.getName() );

        links.add( new BookmarkableMenuLink( RenderUtil.getPageClass( "issues/create" ), addIssueParams, "add issue" ) );
        links.add( new BookmarkableMenuLink( RenderUtil.getPageClass( "milestones/edit" ), pageParams, "edit" ) );
        if ( milestone.getCompletedDate() == null )
        {
            if ( milestone.getOpenIssues().size() == 0 )
            {
                links.add( new BookmarkableMenuLink( RenderUtil.getPageClass( "milestones/complete" ), pageParams, "complete" ) );
            }
            links.add( new BookmarkableMenuLink( RenderUtil.getPageClass( "milestones/comment" ), pageParams, "comment" ) );
        }
        else
        {
            links.add( new BookmarkableMenuLink( RenderUtil.getPageClass( "milestones/reopen" ), pageParams, "reopen" ) );
        }

        return links;
    }
}

class IssueComparator
        implements Comparator<Issue>
{
    public int compare( Issue o1, Issue o2 )
    {
        if ( o1 == null )
        {
            if ( o2 == null )
            {
                return 0;
            }
            return -1;
        }
        if ( o2 == null )
        {
            return 1;
        }

        if ( o1.getPriority() == o2.getPriority() )
        {
            return o1.getStatus() - o2.getStatus();
        }

        return o1.getPriority() - o2.getPriority();
    }
}