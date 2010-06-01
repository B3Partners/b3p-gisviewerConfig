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
package nl.b3p.gis.viewer.zoekconfiguratie;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.viewer.ConfigZoekConfiguratieAction;
import nl.b3p.gis.viewer.ViewerCrudAction;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Attribuut;
import nl.b3p.zoeker.configuratie.ResultaatAttribuut;
import nl.b3p.zoeker.configuratie.ZoekAttribuut;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
import nl.b3p.zoeker.services.Zoeker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.geotools.data.DataStore;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy Braam
 * Created on 31-mei-2010, 12:29:12
 */
public class ConfigZoekConfiguratieVeldAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigZoekConfiguratieAction.class);
    private static final String ZOEKATTRIBUUTID = "zoekAttribuutId";
    private static final String RESULTAATATTRIBUUTID = "resultaatAttribuutId";

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Attribuut attr = getAttribuut(request,false);
        if (attr != null) {
            populateForm(attr, dynaForm);
        }
        return super.unspecified(mapping, dynaForm, request, response);
    }

    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Attribuut attr=populateObject(dynaForm,request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        sess.saveOrUpdate(attr);
        sess.flush();
        request.setAttribute("doClose", true);
        return super.save(mapping, dynaForm, request, response);

    }

    @Override
    public void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws IOException {
        Attribuut a = getAttribuut(request,false);
        DataStore ds = Zoeker.getDataStore(a.getZoekConfiguratie().getBron());
        SimpleFeatureType sft = ds.getSchema(a.getZoekConfiguratie().getFeatureType());

        List<AttributeDescriptor> descriptors = sft.getAttributeDescriptors();
        String[] attributen = new String[descriptors.size()];
        for (int i = 0; i < descriptors.size(); i++) {
            attributen[i] = descriptors.get(i).getName().toString();
        }
        request.setAttribute("attribuutNamen", attributen);

    }
    private Attribuut populateObject(DynaValidatorForm dynaForm,HttpServletRequest request){
        Attribuut attr= getAttribuut(request, true);
        attr.setAttribuutnaam(dynaForm.getString("attribuutnaam"));
        attr.setLabel(dynaForm.getString("label"));
        attr.setNaam(dynaForm.getString("naam"));
        if (FormUtils.nullIfEmpty(dynaForm.getString("type"))!=null)
            attr.setType(new Integer(dynaForm.getString("type")));
        if (FormUtils.nullIfEmpty(dynaForm.getString("volgorde"))!=null)
            attr.setVolgorde(new Integer(dynaForm.getString("volgorde")));
        //zet de zoekconfiguratie als het object nieuw is
        if (attr.getId()==null){
            String zid=request.getParameter("zoekConfiguratieId");
            if (zid!=null){
                Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
                attr.setZoekConfiguratie((ZoekConfiguratie) sess.get(ZoekConfiguratie.class,new Integer(zid)));
            }
        }
        return attr;

    }
    private void populateForm(Attribuut a, DynaValidatorForm dynaForm) {
        if (a instanceof ZoekAttribuut)
            dynaForm.set("zoekAttribuutId", a.getId().toString());
        else if (a instanceof ResultaatAttribuut)
            dynaForm.set("resultaatAttribuutId", a.getId().toString());
        dynaForm.set("label", a.getLabel());
        dynaForm.set("naam", a.getNaam());
        if (a.getType() != null) {
            dynaForm.set("type", a.getType().toString());
        }
        if (a.getVolgorde() != null) {
            dynaForm.set("volgorde", a.getVolgorde().toString());
        }
        dynaForm.set("attribuutnaam", a.getAttribuutnaam());

    }

    private Attribuut getAttribuut(HttpServletRequest request,boolean createNew) {
        Attribuut attribuut = null;
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        if (FormUtils.nullIfEmpty(request.getParameter(ZOEKATTRIBUUTID)) != null) {
            Integer id = new Integer(request.getParameter(ZOEKATTRIBUUTID));
            attribuut = (ZoekAttribuut) sess.get(ZoekAttribuut.class, id);
        } else if (FormUtils.nullIfEmpty(request.getParameter(RESULTAATATTRIBUUTID)) != null) {
            Integer id = new Integer(request.getParameter(RESULTAATATTRIBUUTID));
            attribuut = (ResultaatAttribuut) sess.get(ResultaatAttribuut.class, id);
        }else if(createNew){
            if ("zoek".equalsIgnoreCase(request.getParameter("attribuutType"))){
                return new ZoekAttribuut();
            }else{
                return new ResultaatAttribuut();
            }
        }
        return attribuut;
    }

}
