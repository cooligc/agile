/*
 * HeadsUp Agile
 * Copyright 2013 Heads Up Development Ltd.
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

package org.headsupdev.agile.app.milestones.rest;

import org.apache.wicket.model.util.ListModel;
import org.headsupdev.agile.api.Permission;
import org.headsupdev.agile.app.milestones.dao.MilestonesDAO;
import org.headsupdev.agile.app.milestones.permission.MilestoneListPermission;
import org.headsupdev.agile.storage.StoredProject;
import org.headsupdev.agile.storage.issues.Milestone;
import org.headsupdev.agile.web.MountPoint;
import org.headsupdev.agile.web.rest.HeadsUpApi;

import org.apache.wicket.PageParameters;

/**
 * A milestones API that provides a simple list of milestones per project.
 * <p/>
 * Created: 16/02/2013
 *
 * @author Andrew Williams
 * @since 2.0
 */
@MountPoint( "milestones" )
public class MilestonesApi
        extends HeadsUpApi
{
    private MilestonesDAO dao = new MilestonesDAO();

    public MilestonesApi( PageParameters params )
    {
        super( params );
    }

    @Override
    public Permission getRequiredPermission()
    {
        return new MilestoneListPermission();
    }

    @Override
    public void doGet( PageParameters params )
    {
        if ( getProject().equals( StoredProject.getDefault() ) )
        {
            setModel( new ListModel<Milestone>( dao.findAll() ) );
        }
        else
        {
            setModel( new ListModel<Milestone>( dao.findAll( getProject() ) ) );
        }
    }
}