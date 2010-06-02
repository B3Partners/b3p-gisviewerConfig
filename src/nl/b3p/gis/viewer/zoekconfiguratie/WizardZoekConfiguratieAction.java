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

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.ConfigZoekConfiguratieAction;
import nl.b3p.gis.viewer.ViewerCrudAction;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Bron;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionRedirect;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy Braam
 * Created on 01-juni-2010, 16:10:12
 */
public class WizardZoekConfiguratieAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigZoekConfiguratieAction.class);
    private static final String BRONID = "bronId";
    private static final String FEATURETYPE="featureType";
    //forwards
    public static final String STEP1 = "step1";
    public static final String STEP2 = "step2";
    public static final String STEP3 = "step3";
    public static final String STEP4 = "step4";
    public static final String STEP5 = "step5";

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();
        ExtendedMethodProperties crudProp = new ExtendedMethodProperties(STEP1);
        crudProp.setDefaultForwardName(STEP1);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(STEP1, crudProp);

        crudProp = new ExtendedMethodProperties(STEP2);
        crudProp.setDefaultForwardName(STEP2);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(STEP2, crudProp);

        crudProp = new ExtendedMethodProperties(STEP3);
        crudProp.setDefaultForwardName(STEP3);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(STEP3, crudProp);

        crudProp = new ExtendedMethodProperties(STEP4);
        crudProp.setDefaultForwardName(STEP4);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(STEP4, crudProp);

        crudProp = new ExtendedMethodProperties(STEP5);
        crudProp.setDefaultForwardName(STEP5);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(STEP5, crudProp);

        return map;
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List bronnen=sess.createQuery("from Bron").list();
        request.setAttribute("bronnen", bronnen);
        return super.unspecified(mapping, dynaForm, request, response);
    }

    public ActionForward step1(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {        
        if("nieuw".equalsIgnoreCase(request.getParameter("nieuwofbestaandebron"))){
            return mapping.findForward(STEP2);
        }else{
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            List bronnen=sess.createQuery("from Bron").list();
            request.setAttribute("bronnen", bronnen);
            return mapping.findForward(STEP3);
            
        }
    }
    public ActionForward step2(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
return super.unspecified(mapping, dynaForm, request, response);
    }
    public ActionForward step3(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if(FormUtils.nullIfEmpty(request.getParameter(BRONID))==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"U dient een bron te selecteren.");
            return step1(mapping, dynaForm, request, response);
        }                
        Bron bron=getAndSetBron(request);
        String[] types=ZoekConfiguratieListUtil.getTypeNames(bron,true);
        request.setAttribute("featureTypes",types);
        return mapping.findForward(STEP4);
    }
    public ActionForward step4(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Bron bron=getAndSetBron(request);
        if (bron==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"We zijn vergeten welke bron u geselecteerd heeft, selecteer opnieuw een bron.");
            return step1(mapping,dynaForm,request,response);
        }else if (FormUtils.nullIfEmpty(request.getParameter(FEATURETYPE))==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"U dient featureType/tabel te selecteren.");
            return step3(mapping,dynaForm,request,response);
        }
        request.setAttribute(FEATURETYPE,request.getParameter(FEATURETYPE));
        return mapping.findForward(STEP5);
    }
    public ActionForward step5(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Bron bron=getBron(request);
        if (bron==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"We zijn vergeten welke bron u geselecteerd heeft, selecteer opnieuw een bron.");
            return step1(mapping,dynaForm,request,response);
        }else if (FormUtils.nullIfEmpty(request.getParameter(FEATURETYPE))==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"U dient featureType/tabel te selecteren.");
            return step3(mapping,dynaForm,request,response);
        }
        ZoekConfiguratie zc= new ZoekConfiguratie();
        zc.setNaam(request.getParameter("naam"));
        zc.setParentBron(bron);
        zc.setFeatureType(request.getParameter(FEATURETYPE));
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        sess.save(zc);
        sess.flush();
        //set de juiste waarden om de juiste pagina te openen om te editen.
        ActionRedirect redirect = new ActionRedirect(mapping.findForward("wizardDone"));
        redirect.addParameter(ConfigZoekConfiguratieAction.ZOEKCONFIGURATIEID, zc.getId().toString());
        redirect.addParameter(EDIT,"submit");
        return redirect;        
    }

    private Bron getAndSetBron(HttpServletRequest request){
        Bron bron= getBron(request);
        request.setAttribute(BRONID, bron.getId());
        return bron;
    }
    private Bron getBron(HttpServletRequest request) {
        if (FormUtils.nullIfEmpty(request.getParameter(BRONID))==null){
            return null;
        }
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        Bron b=(Bron) sess.get(Bron.class,new Integer(request.getParameter("bronId")));
        return b;
    }
}
