/*
 * HeadsUp Agile
 * Copyright 2014 Heads Up Development Ltd.
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

import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.StringValueConversionException;
import org.headsupdev.agile.api.Event;
import org.headsupdev.agile.api.Permission;
import org.headsupdev.agile.app.issues.event.UpdateIssueEvent;
import org.headsupdev.agile.app.issues.permission.IssueEditPermission;
import org.headsupdev.agile.storage.Comment;
import org.headsupdev.agile.storage.HibernateStorage;
import org.headsupdev.agile.storage.issues.Issue;
import org.headsupdev.agile.web.BookmarkableMenuLink;
import org.headsupdev.agile.web.HeadsUpPage;
import org.headsupdev.agile.web.MountPoint;

import java.util.Date;
import java.util.Iterator;

/**
 * Created by Gordon Edwards on 08/07/2014.
 */


@MountPoint("editComment")
public class EditComment
        extends HeadsUpPage

{
    private String submitLabel = "Edit Comment";

    private Issue issue;
    private long commentId;
    protected Comment create = new Comment();

    public Permission getRequiredPermission()
    {
        return new IssueEditPermission();
    }

    @Override
    public void layout()
    {
        super.layout();
        add( CSSPackageResource.getHeaderContribution( getClass(), "issue.css" ) );

        long issueId;
        try
        {
            issueId = getPageParameters().getInt( "id" );
        }
        catch ( NumberFormatException e )
        {
            notFoundError();
            return;
        }
        try
        {
            commentId = getPageParameters().getInt( "commentId" );
        }
        catch ( StringValueConversionException e )
        {
            
        }
        Issue issue = IssuesApplication.getIssue( issueId, getProject() );

        if ( issue == null )
        {
            notFoundError();
            return;
        }

        this.issue = issue;
        add( new CommentForm( "comment" ) );
        addLink( new BookmarkableMenuLink( getPageClass( "issues/view" ), getPageParameters(), "view" ) );

    }

    @Override
    public String getTitle()
    {
        return "Edit Comment";
    }

    public Issue getIssue()
    {
        return issue;
    }


    protected void layoutChild( Form form )
    {
    }

    protected void submitChild( Comment comment )
    {
    }

    protected boolean willChildConsumeComment()
    {
        return false;
    }


    public void setSubmitLabel( String submitLabel )
    {
        this.submitLabel = submitLabel;
    }

    protected Event getUpdateEvent( Comment comment )
    {
        return new UpdateIssueEvent( issue, issue.getProject(), getSession().getUser(), comment, "commented on" );
    }

    class CommentForm
            extends Form<Comment>
    {
        private TextArea input;
//        private Comment create = new Comment();

        public CommentForm( String id )
        {
            super( id );
            for ( Comment comment : issue.getComments() )
            {
                if ( comment.getId() == commentId )
                {
                    create.setComment( comment.getComment() );
                    break;
                }
            }

            setModel( new CompoundPropertyModel<Comment>( create ) );
            input = new TextArea( "comment" );
            add( input );
            layoutChild( this );

            add( new Button( "submit", new Model<String>()
            {
                public String getObject()
                {
                    return submitLabel;
                }
            } ) );
        }

        public void onSubmit()
        {
            issue = (Issue) ( (HibernateStorage) getStorage() ).getHibernateSession().merge( issue );

            Date now = new Date();
            Date created = new Date();
            if ( create.getComment() != null )
            {
                create.setUser( EditComment.this.getSession().getUser() );

                Iterator<Comment> iterator = issue.getComments().iterator();
                while ( iterator.hasNext() )
                {
                    Comment comment = iterator.next();
                    if ( comment.getId() == commentId )
                    {
                        created = comment.getCreated();
                        iterator.remove();
                        break;
                    }
                }
                create.setCreated( created );
                create.setComment( input.getInput() );
                ( (HibernateStorage) getStorage() ).save( create );
                if ( !willChildConsumeComment() )
                {
                    issue.addComment( create );
                }
            }

            submitChild( create );

            if ( issue.getStatus() < Issue.STATUS_FEEDBACK )
            {
                issue.setStatus( Issue.STATUS_FEEDBACK );
            }
            // this line is needed by things that extend our form...
            issue.setUpdated( now );
            getHeadsUpApplication().addEvent( getUpdateEvent( create ) );

            setResponsePage( getPageClass( "issues/view" ), getPageParameters() );
        }
    }
}
