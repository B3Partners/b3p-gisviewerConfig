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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.ConfigZoekConfiguratieAction;
import nl.b3p.gis.viewer.ViewerCrudAction;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.ZoekconfiguratieThemas;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Bron;
import nl.b3p.zoeker.configuratie.ResultaatAttribuut;
import nl.b3p.zoeker.configuratie.ZoekAttribuut;
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
 *
 * @author Roy Braam Created on 01-juni-2010, 16:10:12
 */
public class WizardZoekConfiguratieAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigZoekConfiguratieAction.class);
    private static final String BRONID = "bronId";
    private static final String FEATURETYPE = "featureType";
    private static final String PARENTZOEKCONFIGURATIE = "parentZoekConfiguratie";
    private static final String ZOEKCONFIGURATIEID = "zoekConfiguratieId";
    //forwards
    public static final String STEP1 = "step1";
    public static final String STEP2 = "step2";
    public static final String STEP3 = "step3";
    public static final String STEP4 = "step4";
    public static final String STEP5 = "step5";
    public static final String SAVESTEP5 = "saveStep5";
    public static final String WIZARDDONE = "wizardDone";
    protected static final String ERROR_ZOEKVELD_RELATION = "error.zoekveld.relation";

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

        crudProp = new ExtendedMethodProperties(STEP5);
        crudProp.setDefaultForwardName(STEP5);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(STEP5, crudProp);

        crudProp = new ExtendedMethodProperties(SAVESTEP5);
        crudProp.setDefaultForwardName(SAVESTEP5);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(SAVESTEP5, crudProp);

        crudProp = new ExtendedMethodProperties(WIZARDDONE);
        crudProp.setDefaultForwardName(WIZARDDONE);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(WIZARDDONE, crudProp);

        return map;
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List bronnen = sess.createCriteria(Bron.class).list();
        request.setAttribute("bronnen", bronnen);
        prepareMethod(dynaForm, request, EDIT, LIST);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (FormUtils.nullIfEmpty(request.getParameter(ZOEKCONFIGURATIEID)) != null) {

            Integer id = new Integer(request.getParameter(ZOEKCONFIGURATIEID));
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

            Object zc = sess.get(ZoekConfiguratie.class, id);

            if (zc != null) {
                ZoekConfiguratie zoekConfiguratie = (ZoekConfiguratie) zc;

                String message = "";

                /*
                 * controleren of er een zoekconfig bestaat die de te
                 * verwijderen zoekconfig gekoppeld heeft als vervolgzoeker
                 */
                List parents = sess.createQuery("from ZoekConfiguratie z where z.parentZoekConfiguratie=:zc").setParameter("zc", zc).list();

                if (parents != null && parents.size() > 0) {
                    message += "De zoekconfiguratie(s) ";

                    Iterator iter = parents.iterator();
                    while (iter.hasNext()) {
                        ZoekConfiguratie zoekConfig = (ZoekConfiguratie) iter.next();

                        message += " : " + zoekConfig.getNaam();
                    }

                    message += ", hebben de " + zoekConfiguratie.getNaam() + " zoeker nog als vervolgzoeker ingesteld. ";
                }

                /*
                 * controleren of er een zoekconfig bestaat die de te
                 * verwijderen zoekconfig gekoppeld heeft via een zoekveld input
                 */
                List velden = sess.createQuery("from ZoekAttribuut zv where zv.zoekConfiguratie != :zcId1 AND "
                        + "zv.inputzoekconfiguratie = :zcId2").setParameter("zcId1", zc).setParameter("zcId2", zc).list();

                if (velden != null && velden.size() > 0) {
                    message += "In de zoekconfiguratie(s) ";

                    Iterator iter = velden.iterator();
                    while (iter.hasNext()) {
                        ZoekAttribuut za = (ZoekAttribuut) iter.next();

                        message += " : " + za.getZoekConfiguratie().getNaam();
                    }

                    message += ", wordt de " + zoekConfiguratie.getNaam() + " zoeker nog als input voor een zoekveld gebruikt.";
                }

                int size = parents.size() + velden.size();

                if (size > 0) {
                    addAlternateMessage(mapping, request, ERROR_ZOEKVELD_RELATION, message);

                    return step2(mapping, dynaForm, request, response);
                } else {
                    sess.delete(zc);
                    sess.flush();
                }

            } else { // zc is null
                addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "Kan opgegeven zoekconfiguratie niet vinden.");
            }
        }

        return mapping.findForward("wizardDone");
    }

    public ActionForward step1(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (FormUtils.nullIfEmpty(request.getParameter(BRONID)) == null) {
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "U dient een bron te selecteren.");
            return unspecified(mapping, dynaForm, request, response);
        }
        if ("new".equalsIgnoreCase(request.getParameter(BRONID))) {
            ActionRedirect redirect = new ActionRedirect(mapping.findForward("wizardCreateBron"));
            return redirect;
        }
        Bron bron = getAndSetBron(request);
        String[] types = ZoekConfiguratieListUtil.getTypeNames(bron, true);
        request.setAttribute("featureTypes", types);

        return mapping.findForward(STEP2);
    }

    public ActionForward step2(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        /*
         * kijk of er een zoekconfiguratie is, zoja vul dan alles met de
         * gegevens van de zoekconfiguratie Zoniet vul dan alles met de hiervoor
         * geselecteerde dingen.
         */
        ZoekConfiguratie zc = getAndSetZoekConfiguratie(request);
        Bron bron = null;
        String featureType = null;
        if (zc != null) {
            request.setAttribute(ZOEKCONFIGURATIEID, zc.getId());
            bron = zc.getBron();
            if (zc.getBron() != null) {
                request.setAttribute(BRONID, bron.getId());
            }
            request.setAttribute("naam", zc.getNaam());
            if (zc.getParentZoekConfiguratie() != null) {
                request.setAttribute(PARENTZOEKCONFIGURATIE, zc.getParentZoekConfiguratie().getId());
            }

            if (!zc.isResultListDynamic()) {
                request.setAttribute("usecaching", "1");
            }

            featureType = zc.getFeatureType();
        } else {
            bron = getAndSetBron(request);
            featureType = FormUtils.nullIfEmpty(request.getParameter(FEATURETYPE));
        }

        request.setAttribute(FEATURETYPE, featureType);

        if (zc != null && bron == null || featureType == null) {
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "De geselecteerde zoekconfiguratie is ongeldig.");
            return unspecified(mapping, dynaForm, request, response);
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        //maak een lijst met mogelijke zoekconfiguraties (om als parent te kiezen)zonder zichzelf
        String queryString = "from ZoekConfiguratie";
        if (zc != null) {
            queryString += " z where z.id != " + zc.getId();
        }
        List zoekconfiguraties = sess.createQuery(queryString).list();
        request.setAttribute("zoekConfiguraties", zoekconfiguraties);
        return mapping.findForward(STEP3);
    }

    /*
     * Zoekingang velden
     */
    public ActionForward step3(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ZoekConfiguratie zc = getAndSetZoekConfiguratie(request);
        Bron bron = null;
        String featureType = null;
        String naam = null;

        if (zc != null) {
            bron = zc.getBron();
            featureType = zc.getFeatureType();
            if (zc.getNaam() == null || zc.getNaam().equals("")) {
                naam = FormUtils.nullIfEmpty(request.getParameter("naam"));
            } else {
                naam = zc.getNaam();
            }
        } else {
            bron = getAndSetBron(request);
            featureType = FormUtils.nullIfEmpty(request.getParameter(FEATURETYPE));
            naam = FormUtils.nullIfEmpty(request.getParameter("naam"));
        }

        if (zc != null && (bron == null || featureType == null)) {
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "De zoekingang is ongeldig.");
            return unspecified(mapping, dynaForm, request, response);
        }

        if (bron == null) {
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "De zoekingang heeft geen bron. Selecteer een bron.");
            return unspecified(mapping, dynaForm, request, response);
        } else if (featureType == null) {
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "De zoekingang heeft geen tabel/feature. Selecteer een tabel/feature.");
            return step1(mapping, dynaForm, request, response);
        } else if (naam == null) {
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "Geef een naam op voor deze zoekingang.");
            return step2(mapping, dynaForm, request, response);
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        //maak de zoekconfiguratie als die nog niet bestaat
        if (zc == null) {
            zc = new ZoekConfiguratie();
        }
        //kijk of er een zoek configuratie is geselecteerd.
        if (FormUtils.StringToInteger(request.getParameter(PARENTZOEKCONFIGURATIE)) != null) {
            Integer parentId = FormUtils.StringToInteger(request.getParameter(PARENTZOEKCONFIGURATIE));
            ZoekConfiguratie parent = (ZoekConfiguratie) sess.get(ZoekConfiguratie.class, parentId);
            zc.setParentZoekConfiguratie(parent);
        } else if (request.getParameter(PARENTZOEKCONFIGURATIE) != null) {
            zc.setParentZoekConfiguratie(null);
        }
        if (FormUtils.nullIfEmpty(request.getParameter("naam")) != null) {
            zc.setNaam(request.getParameter("naam"));
        }
        zc.setParentBron(bron);
        zc.setFeatureType(featureType);

        String useCaching = FormUtils.nullIfEmpty(request.getParameter("usecaching"));

        if (useCaching != null) {
            zc.setResultListDynamic(false);
            request.setAttribute("usecaching", "1");
        } else {
            zc.setResultListDynamic(true);
        }

        //sla alles op.
        sess.save(zc);
        sess.flush();
        //set the lijsten die nodig zijn voor de volgende pagina.
        request.setAttribute("zoekConfiguratieId", zc.getId());
        request.setAttribute("zoekVelden", zc.getZoekVelden());
        request.setAttribute("resultaatVelden", zc.getResultaatVelden());
        request.setAttribute("tips", createTips(zc));
        if (zc.getParentZoekConfiguratie() != null) {
            request.setAttribute("parentZoekVelden", zc.getParentZoekConfiguratie().getZoekVelden());
        }
        return mapping.findForward(STEP4);
    }

    public ActionForward step5(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ZoekConfiguratie zc = getAndSetZoekConfiguratie(request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List l = sess.createQuery("from Themas").list();

        List<ZoekconfiguratieThemas> zoekconfigThemas = sess.createQuery("from ZoekconfiguratieThemas WHERE zoekconfiguratie = :id").setParameter("id", zc).list();
        List<String> layersAan = new ArrayList<String>();
        List<String> layerNamesOn = new ArrayList<String>();
        for (Iterator<ZoekconfiguratieThemas> it = zoekconfigThemas.iterator(); it.hasNext();) {
            ZoekconfiguratieThemas zoekconfiguratieThema = it.next();
            layersAan.add(zoekconfiguratieThema.getThema().getId().toString());
            layerNamesOn.add(zoekconfiguratieThema.getThema().getNaam());
        }
        request.setAttribute("themas", l);
        request.setAttribute("layerNamesOn", layerNamesOn);
        dynaForm.set("layersAan", layersAan.toArray(new String[layersAan.size()]));
        return mapping.findForward(STEP5);
    }

    public ActionForward saveStep5(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ZoekConfiguratie zc = getAndSetZoekConfiguratie(request);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        // Verwijder de oude entries
        
        List<ZoekconfiguratieThemas> zoekconfigThemas = sess.createQuery("from ZoekconfiguratieThemas WHERE zoekconfiguratie = :id").setParameter("id", zc).list();
        for (Iterator<ZoekconfiguratieThemas> it = zoekconfigThemas.iterator(); it.hasNext();) {
            ZoekconfiguratieThemas zoekconfigThema = it.next();
            sess.delete(zoekconfigThema);
        }
        sess.flush();
        String[] layersAan = dynaForm.getStrings("layersAan");

        for (int i = 0; i < layersAan.length; i++) {
            String thema = layersAan[i];
            Themas t = (Themas) sess.get(Themas.class, new Integer(thema));
            ZoekconfiguratieThemas zt = new ZoekconfiguratieThemas();
            zt.setThema(t);
            zt.setZoekconfiguratie(zc);
            sess.save(zt);
        }
        sess.flush();
        return mapping.findForward(WIZARDDONE);
    }

    /**
     * Haalt de bron op met het id op het request en set het ook weer gelijk op
     * het request zodat de volgende submit het weer kan doorsturen
     */
    private Bron getAndSetBron(HttpServletRequest request) {
        Bron bron = getBron(request);
        if (bron == null) {
            return null;
        }
        request.setAttribute(BRONID, bron.getId());
        return bron;
    }

    private Bron getBron(HttpServletRequest request) {
        if (FormUtils.nullIfEmpty(request.getParameter(BRONID)) == null) {
            return null;
        }
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Bron b = (Bron) sess.get(Bron.class, new Integer(request.getParameter("bronId")));
        return b;
    }

    /**
     * Haalt de ZoekConfiguratie op met het id op het request en set het ook
     * weer gelijk op het request zodat de volgende submit het weer kan
     * doorsturen
     */
    private ZoekConfiguratie getAndSetZoekConfiguratie(HttpServletRequest request) {
        Integer zid = FormUtils.StringToInteger(request.getParameter(ZOEKCONFIGURATIEID));
        if (zid == null) {
            return null;
        }
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        ZoekConfiguratie zc = (ZoekConfiguratie) sess.get(ZoekConfiguratie.class, zid);
        if (zc != null) {
            request.setAttribute(ZOEKCONFIGURATIEID, zc.getId());
        }
        return zc;
    }
    /*
     * Maak een lijst met tip resource keys zodat de gebruiker wat feedback
     * krijgt bij het aanmaken van de velden.
     */

    private ArrayList<String> createTips(ZoekConfiguratie zc) {
        if (zc == null) {
            return null;
        }
        ArrayList<String> tips = new ArrayList();
        if (zc.getZoekVelden() == null || zc.getZoekVelden().size() == 0) {
            tips.add("wizardzoekconfiguratie.tip.geenzoekvelden");
        }
        if (zc.getResultaatVelden() == null || zc.getResultaatVelden().size() == 0) {
            tips.add("wizardzoekconfiguratie.tip.geenresultaatvelden");
        }
        //als er zoek veldne zijn, controleer dan op de bruikbaarheid daarvan.
        if (zc.getZoekVelden() != null && zc.getZoekVelden().size() == 0) {
            Iterator<ZoekAttribuut> it = zc.getZoekVelden().iterator();
            boolean geometryExists = false;
            boolean geometryDependentExists = false;
            while (it.hasNext()) {
                ZoekAttribuut za = it.next();
                if (ZoekAttribuut.GEOMETRY_TYPE == za.getType()) {
                    geometryExists = true;
                } else if (ZoekAttribuut.STRAAL_TYPE == za.getType()) {
                    geometryDependentExists = true;
                }
            }
            if (geometryDependentExists && !geometryExists) {
                tips.add("wizardzoekconfiguratie.tip.geengeometriezoek");
            }
        }
        //als er resultaat velden zijn, controleer dan op de bruikbaarheid.
        if (zc.getResultaatVelden() != null && zc.getResultaatVelden().size() > 0) {
            Iterator<ResultaatAttribuut> it = zc.getResultaatVelden().iterator();
            boolean geometryExists = false;
            boolean toonExists = false;
            while (it.hasNext()) {
                ResultaatAttribuut ra = it.next();
                //controleer of er een geometry is opgegeven zodat er kan worden gezoomd
                if (ResultaatAttribuut.GEOMETRY_TYPE == ra.getType()) {
                    geometryExists = true;
                } //kijk of er velden zijn die als resultaat worden getoond.
                else if (ResultaatAttribuut.TOON_TYPE == ra.getType() || ResultaatAttribuut.ALLEEN_TOON_TYPE == ra.getType()) {
                    toonExists = true;
                }
            }
            if (!geometryExists) {
                tips.add("wizardzoekconfiguratie.tip.geengeometrieresultaat");
            }
            if (!toonExists) {
                tips.add("wizardzoekconfiguratie.tip.geentoonresultaat");
            }
        }
        return tips;
    }
}
