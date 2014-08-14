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

package org.headsupdev.agile.app.issues;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.headsupdev.agile.api.Manager;
import org.headsupdev.agile.api.Project;
import org.headsupdev.agile.api.Storage;
import org.headsupdev.agile.api.User;
import org.headsupdev.agile.app.issues.permission.IssueEditPermission;
import org.headsupdev.agile.storage.Comment;
import org.headsupdev.agile.storage.HibernateStorage;
import org.headsupdev.agile.storage.issues.Issue;
import org.headsupdev.agile.storage.resource.DurationWorked;
import org.headsupdev.agile.web.HeadsUpPage;
import org.headsupdev.agile.web.HeadsUpSession;
import org.headsupdev.agile.web.components.FormattedDateModel;
import org.headsupdev.agile.web.components.MarkedUpTextModel;

import java.util.Date;
import java.util.List;

/**
 * TODO: Document me
 * <p/>
 * Created: 05/02/2012
 *
 * @author Andrew Williams
 * @since 1.0
 */
public class CommentPanel
        extends Panel
{
    private HeadsUpPage page;
    private Issue issue;
    private List commentList;
    private Project project;

    private Storage storage;
    private DurationWorked duration;
    private Comment comment;

    public CommentPanel( String id, Comment comment, Project project )
    {
        super( id, new Model( comment ) );
        this.project = project;

        layout();
    }

    public CommentPanel( String id, DurationWorked worked, Project project )
    {
        super( id, new Model( worked ) );
        this.project = project;

        layout();
    }

    public CommentPanel( String id, IModel model, Project project, List commentList, Issue issue, HeadsUpPage page )
    {
        super( id, model );
        this.project = project;
        this.commentList = commentList;
        this.issue = issue;
        this.page = page;
        this.storage = page.getStorage();
        layout();
    }

    private void layout()
    {
        User currentUser = ( (HeadsUpSession) getSession() ).getUser();
        boolean userHasPermission = Manager.getSecurityInstance().userHasPermission( currentUser, new IssueEditPermission(), project );
        Object o = getDefaultModel().getObject();

        WebMarkupContainer commentTitle = new WebMarkupContainer( "comment-title" );
        WebMarkupContainer workedTitle = new WebMarkupContainer( "worked-title" );
        if ( o instanceof Comment )
        {
            comment = (Comment) o;
            add( new Image( "icon", new ResourceReference( HeadsUpPage.class, "images/comment.png" ) ) );

            PageParameters params = page.getProjectPageParameters();
            params.put( "id", issue.getId() );
            params.put( "commentId", comment.getId() );
            Link edit = new BookmarkablePageLink( "editComment", EditComment.class, params );

            commentTitle.add( edit.setVisible( userHasPermission ) );
            commentTitle.add( new Label( "username", comment.getUser().getFullnameOrUsername() ) );
            commentTitle.add( new Label( "created", new FormattedDateModel( comment.getCreated(),
                    ( (HeadsUpSession) getSession() ).getTimeZone() ) ) );
            add( new Label( "comment", new MarkedUpTextModel( comment.getComment(), project ) )
                    .setEscapeModelStrings( false ) );
            Link remove = new Link( "removeComment" )
            {
                @Override
                public void onClick()
                {
                    Comment comm = (Comment) ( (HibernateStorage) storage ).merge( comment );
                    issue.getComments().remove( comm );
                    Issue iss = (Issue) ( (HibernateStorage) storage ).merge( issue );
                    commentList.remove( comm );
                    iss.setUpdated( new Date() );
                    ( (HibernateStorage) storage ).delete( comm );
                }
            };
            commentTitle.add( remove.setVisible( userHasPermission ) );
            workedTitle.setVisible( false );
        }
        else if ( o instanceof DurationWorked )
        {
            duration = (DurationWorked) o;
            add( new Image( "icon", new ResourceReference( HeadsUpPage.class, "images/worked.png" ) ) );

            PageParameters params = page.getProjectPageParameters();
            params.put( "id", issue.getId() );
            params.put( "durationId", duration.getId() );
            Link edit = new BookmarkablePageLink( "editComment", EditProgressIssue.class, params );
            workedTitle.add( edit.setVisible( userHasPermission ) );

            Link remove = new Link( "removeComment" )
            {
                @Override
                public void onClick()
                {
                    DurationWorked dur = (DurationWorked) ( (HibernateStorage) storage ).merge( duration );
                    issue.getTimeWorked().remove( dur );
                    issue.setUpdated( new Date() );
                    commentList.remove( dur );
                    dur.setIssue( null );
                    ( (HibernateStorage) storage ).delete( dur );
                }
            };
            workedTitle.add( remove.setVisible( userHasPermission ) );
            DurationWorked worked = (DurationWorked) o;
            if ( worked.getWorked() == null || worked.getWorked().getHours() == 0 )
            {
                setVisible( false );
                return;
            }
            String time = "";
            if ( worked.getWorked() != null )
            {
                time = worked.getWorked().toString();
            }
            workedTitle.add( new Label( "worked", time ) );
            workedTitle.add( new Label( "username", worked.getUser().getFullnameOrUsername() ) );
            workedTitle.add( new Label( "created", new FormattedDateModel( worked.getDay(),
                    ( (HeadsUpSession) getSession() ).getTimeZone() ) ) );

            commentTitle.setVisible( false );

            Comment comment = worked.getComment();
            if ( comment != null )
            {
                Label commentLabel = new Label( "comment", new MarkedUpTextModel( comment.getComment(), project ) );
                commentLabel.setEscapeModelStrings( false );
                add( commentLabel );
            }
            else
            {
                add( new WebMarkupContainer( "comment" ).setVisible( false ) );
            }
        }
        else
        {
            commentTitle.setVisible( false );
            workedTitle.setVisible( false );
            add( new WebMarkupContainer( "comment" ).setVisible( false ) );
        }
        add( commentTitle );
        add( workedTitle );
    }
}