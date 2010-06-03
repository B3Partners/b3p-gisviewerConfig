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
    private static final String PARENTZOEKCONFIGURATIE="parentZoekConfiguratie";
    private static final String ZOEKCONFIGURATIEID="zoekConfiguratieId";
    //forwards
    public static final String STEP1 = "step1";
    public static final String STEP2 = "step2";
    public static final String STEP3 = "step3";
    public static final String STEP4 = "step4";

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
        if(FormUtils.nullIfEmpty(request.getParameter(BRONID))==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"U dient een bron te selecteren.");
            return step1(mapping, dynaForm, request, response);
        }
        if ("new".equalsIgnoreCase(request.getParameter(BRONID))){
            ActionRedirect redirect = new ActionRedirect(mapping.findForward("wizardCreateBron"));
            return redirect;
        }
        Bron bron=getAndSetBron(request);
        String[] types=ZoekConfiguratieListUtil.getTypeNames(bron,true);
        request.setAttribute("featureTypes",types);
        return mapping.findForward(STEP2);
    }   
    public ActionForward step2(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        /*kijk of er een zoekconfiguratie is, zoja vul dan alles met de gegevens van de zoekconfiguratie
        Zoniet vul dan alles met de hiervoor geselecteerde dingen.*/
        ZoekConfiguratie zc= getAndSetZoekConfiguratie(request);
        Bron bron=null;
        String featureType=null;
        if (zc!=null){
            request.setAttribute(ZOEKCONFIGURATIEID, zc.getId());
            bron=zc.getBron();
            if (zc.getBron()!=null)
                request.setAttribute(BRONID,bron.getId());
            request.setAttribute("naam",zc.getNaam());
            if (zc.getParentZoekConfiguratie()!=null)
                request.setAttribute(PARENTZOEKCONFIGURATIE,zc.getParentZoekConfiguratie().getId());
            featureType=zc.getFeatureType();
        }else{
            bron=getAndSetBron(request);            
            featureType=FormUtils.nullIfEmpty(request.getParameter(FEATURETYPE));
        }
        request.setAttribute(FEATURETYPE,featureType);
        //controleer of er een bron en featuretype is.
        if (bron==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"We zijn vergeten welke bron u geselecteerd heeft, selecteer opnieuw een bron.");
            return step1(mapping,dynaForm,request,response);
        }else if (featureType==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"U dient featureType/tabel te selecteren.");
            return step1(mapping,dynaForm,request,response);
        }        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        //maak een lijst met mogelijke zoekconfiguraties (om als parent te kiezen)zonder zichzelf
        String queryString="from ZoekConfiguratie";
        if (zc!=null){
            queryString+=" z where z.id != "+zc.getId();
        }
        List zoekconfiguraties = sess.createQuery(queryString).list();
        request.setAttribute("zoekConfiguraties",zoekconfiguraties);        
        return mapping.findForward(STEP3);
    }
    public ActionForward step3(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ZoekConfiguratie zc= getAndSetZoekConfiguratie(request);
        Bron bron=null;
        String featureType=null;
        if (zc!=null){
            bron=zc.getBron();
            featureType=zc.getFeatureType();
        }else{
            bron=getAndSetBron(request);
            featureType=FormUtils.nullIfEmpty(request.getParameter(FEATURETYPE));
        }
        if (bron==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"We zijn vergeten welke bron u geselecteerd heeft, selecteer opnieuw een bron.");
            return unspecified(mapping,dynaForm,request,response);
        }else if (featureType==null){
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY,"U dient featureType/tabel te selecteren.");
            return step1(mapping,dynaForm,request,response);
        }
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        //maak de zoekconfiguratie als die nog niet bestaat
        if (zc==null)
            zc=new ZoekConfiguratie();
        //kijk of er een zoek configuratie is geselecteerd.
        if (FormUtils.StringToInteger(request.getParameter(PARENTZOEKCONFIGURATIE))!=null){
            Integer parentId=FormUtils.StringToInteger(request.getParameter(PARENTZOEKCONFIGURATIE));
            ZoekConfiguratie parent=(ZoekConfiguratie) sess.get(ZoekConfiguratie.class, parentId);
            zc.setParentZoekConfiguratie(parent);
        }else{
            zc.setParentZoekConfiguratie(null);
        }
        zc.setNaam(request.getParameter("naam"));
        zc.setParentBron(bron);
        zc.setFeatureType(featureType);
        //sla alles op.
        sess.save(zc);
        sess.flush();
        //set the lijsten die nodig zijn voor de volgende pagina.
        request.setAttribute("zoekConfiguratieId",zc.getId());
        request.setAttribute("zoekVelden",zc.getZoekVelden());
        request.setAttribute("resultaatVelden",zc.getResultaatVelden());
        
        return mapping.findForward(STEP4);
    }
    /**
     * Haalt de bron op met het id op het request en set het ook weer gelijk op het request
     * zodat de volgende submit het weer kan doorsturen
     */
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
    /**
     * Haalt de ZoekConfiguratie op met het id op het request en set het ook weer gelijk op het request
     * zodat de volgende submit het weer kan doorsturen
     */
    private ZoekConfiguratie getAndSetZoekConfiguratie(HttpServletRequest request){
        Integer zid = FormUtils.StringToInteger(request.getParameter(ZOEKCONFIGURATIEID));
        if (zid==null)
            return null;
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        ZoekConfiguratie zc= (ZoekConfiguratie)sess.get(ZoekConfiguratie.class,zid);
        if (zc!=null)
            request.setAttribute(ZOEKCONFIGURATIEID, zc.getId());
        return zc;
    }
}
