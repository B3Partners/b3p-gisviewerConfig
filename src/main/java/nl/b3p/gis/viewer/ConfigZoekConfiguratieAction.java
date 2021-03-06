/*
 * B3P Gisviewer is an extension to Flamingo MapComponents making
 * it a complete webbased GIS viewer and configuration tool that
 * works in cooperation with B3P Kaartenbalie.
 *
 * Copyright 2006, 2007, 2008 B3Partners BV
 *
 * This file is part of B3P Gisviewer.
 *
 * B3P Gisviewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * B3P Gisviewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with B3P Gisviewer.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

public class ConfigZoekConfiguratieAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigZoekConfiguratieAction.class);

    public static final String ZOEKCONFIGURATIEID = "zoekConfiguratieId";

    @Override
    public void createLists(DynaValidatorForm dynaForm,HttpServletRequest request) {
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekConfiguraties = sess.createQuery("from ZoekConfiguratie zc"
                + " LEFT JOIN FETCH zc.parentBron"
                + " order by zc.naam").list();
        
        request.setAttribute("zoekConfiguraties", zoekConfiguraties);
        
    }

}
