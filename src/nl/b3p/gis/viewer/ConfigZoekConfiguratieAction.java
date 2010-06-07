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
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy Braam
 * Created on 31-mei-2010, 12:29:12
 */
public class ConfigZoekConfiguratieAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigZoekConfiguratieAction.class);
    public static final String ZOEKCONFIGURATIEID="zoekConfiguratieId";
    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        createStartLists(request);
        return super.unspecified(mapping, dynaForm, request, response);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ZoekConfiguratie z= getZoekConfiguratie(request, false);
        if (z!=null){
            populateForm(z,dynaForm);
        }
        request.setAttribute("newObject",true);
        createEditLists(request,z);        
        return super.edit(mapping,dynaForm,request,response);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Session sess=HibernateUtil.getSessionFactory().getCurrentSession();
        ZoekConfiguratie z= populateObject(dynaForm, request);
        //sla de zoekconfiguratie op.
        sess.saveOrUpdate(z);
        sess.flush();
        //maak de start lijst.
        createStartLists(request);
        //roep de super aan maar wacht met returnen
        ActionForward af= super.save(mapping, dynaForm, request, response);
        //reset het formulier.
        dynaForm.initialize(mapping);
        return af;
    }
    public void createEditLists(HttpServletRequest request,ZoekConfiguratie z){
        if (z==null){
            return;
        }
         //zoekconfiguraties
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekConfiguraties =sess.createQuery("from ZoekConfiguratie where id != :id").setParameter("id", z.getId()).list();
        request.setAttribute("zoekConfiguratieList", zoekConfiguraties);

        request.setAttribute("zoekVelden",z.getZoekVelden());
        request.setAttribute("resultaatVelden",z.getResultaatVelden());
    }
    public void createStartLists(HttpServletRequest request){
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekConfiguraties=sess.createQuery("from ZoekConfiguratie").list();
        request.setAttribute("zoekConfiguraties", zoekConfiguraties);
    }

    private ZoekConfiguratie getZoekConfiguratie(HttpServletRequest request, boolean createNew) {
        Integer id = FormUtils.StringToInteger(request.getParameter(ZOEKCONFIGURATIEID));
        ZoekConfiguratie z = null;
        if (id == null && createNew) {
            z = new ZoekConfiguratie();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            z = (ZoekConfiguratie) sess.get(ZoekConfiguratie.class, id);
        }
        return z;
    }

    private void populateForm(ZoekConfiguratie z, DynaValidatorForm dynaForm) {
        dynaForm.set(ZOEKCONFIGURATIEID, z.getId().toString());
        dynaForm.set("naam", z.getNaam());
        dynaForm.set("featureType", z.getFeatureType());
        if (z.getParentBron()!=null)
            dynaForm.set("parentBron", z.getParentBron().toString());
        if (z.getParentZoekConfiguratie()!=null)
            dynaForm.set("parentZoekConfiguratie", z.getParentZoekConfiguratie().toString());
    }

    private ZoekConfiguratie populateObject(DynaValidatorForm dynaForm,HttpServletRequest request){
        ZoekConfiguratie z= getZoekConfiguratie(request, true);
        z.setNaam(dynaForm.getString("naam"));
        if (FormUtils.nullIfEmpty(dynaForm.getString("parentZoekConfiguratie"))!=null){
            Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
            Integer parentId=new Integer(dynaForm.getString("parentZoekConfiguratie"));
            ZoekConfiguratie parent=(ZoekConfiguratie) sess.get(ZoekConfiguratie.class,parentId);
            z.setParentZoekConfiguratie(parent);
        }
        return z;
    }
}
