package nl.b3p.gis.viewer;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.utils.ConfigListsUtil;
import nl.b3p.gis.viewer.db.DataTypen;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.WaardeTypen;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Query;
import org.hibernate.Session;
import org.opengis.feature.type.Name;

/**
 *
 * @author Chris
 */
public class ConfigThemaDataAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigThemaAction.class);
    protected static final String CHANGE = "change";
    protected static final String CREATEALLTHEMADATA = "createAllThemaData";
    protected static final String UPDATEBASISREGELS = "updateBasisregels";
    private int DEFAULTBASISCOLUMNS = 0;

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();

        ExtendedMethodProperties crudProp = null;

        crudProp = new ExtendedMethodProperties(CHANGE);
        crudProp.setDefaultForwardName(SUCCESS);
        map.put(CHANGE, crudProp);

        crudProp = new ExtendedMethodProperties(CREATEALLTHEMADATA);
        crudProp.setDefaultForwardName(SUCCESS);
        map.put(CREATEALLTHEMADATA, crudProp);

        crudProp = new ExtendedMethodProperties(UPDATEBASISREGELS);
        crudProp.setDefaultForwardName(SUCCESS);
        map.put(UPDATEBASISREGELS, crudProp);

        return map;
    }

    protected ThemaData getThemaData(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("themaDataID"));
        ThemaData td = null;
        if (id == null && createNew) {
            td = new ThemaData();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            td = (ThemaData) sess.get(ThemaData.class, id);
        }
        return td;
    }

    protected ThemaData getFirstThemaData(Gegevensbron gb) {
        if (gb == null) {
            return null;
        }
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Query q = sess.createQuery("from ThemaData where gegevensbron = :gb order by dataorder, label");
        List cs = q.setParameter("gb", gb).setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (ThemaData) cs.get(0);
        }
        ThemaData td = new ThemaData();
        //td.setThema(t);
        td.setGegevensbron(gb);
        return td;
    }

    protected Gegevensbron getGegevensbron(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("gegevensbronID"));
        Gegevensbron gb = null;

        if (id == null && createNew) {
            gb = new Gegevensbron();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            gb = (Gegevensbron) sess.get(Gegevensbron.class, id);
        }

        return gb;
    }

    protected Gegevensbron getFirstGegevensbron() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List cs = sess.createQuery("from Gegevensbron order by naam").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Gegevensbron) cs.get(0);
        }
        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        super.createLists(dynaForm, request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        request.setAttribute("listThemas", sess.createQuery("from Gegevensbron order by LOWER(naam) asc").list());
        request.setAttribute("listWaardeTypen", sess.createQuery("from WaardeTypen order by naam").list());
        request.setAttribute("listDataTypen", sess.createQuery("from DataTypen order by naam").list());
        
        Gegevensbron gb;
        ThemaData td = getThemaData(dynaForm, false);
        if (td == null) {
            gb = getGegevensbron(dynaForm, false);
            if (gb == null) {
                gb = getFirstGegevensbron();
            }
        } else {
            gb = td.getGegevensbron();
        }
        if (gb == null) {
            return;
        }

        List<ThemaData> bestaandeObjecten = SpatialUtil.getThemaData(gb, false);
        request.setAttribute("listThemaData", bestaandeObjecten);
        request.setAttribute("gegevensbron", gb);

        Bron b = gb.getBron(request);

        List<String> attributes = new ArrayList();
        try {
            attributes = DataStoreUtil.getAttributeNames(b, gb);
            request.setAttribute("listAdminTableColumns", attributes);
        } catch (SocketTimeoutException e) {
            logger.error("Socket time out error while getting attributes.");
        }

        /* Workaround voor ophalen objectdata voor WFS 110 */
        if (attributes == null || attributes.size() < 1) {
            attributes = getObjectDataForWFS110(b, gb);
        }

        if (gb.getBron() != null) {
            request.setAttribute("connectieType", gb.getBron().getType());
        } else {
            request.setAttribute("connectieType", Bron.TYPE_EMPTY);
        }

        ArrayList<Integer> basisregels = new ArrayList<Integer>();
        ArrayList<Integer> editables = new ArrayList<Integer>();

        Map volgordeVelden = new HashMap();
        Map labelVelden = new HashMap();

        StringBuilder uglyThemaData = new StringBuilder();
        for (ThemaData tdi : bestaandeObjecten) {
            if (tdi.isBasisregel()) {
                basisregels.add(tdi.getId());
            }
            if (tdi.isEditable()) {
                editables.add(tdi.getId());
            }
            boolean bestaatNog = false;
            if (tdi.getKolomnaam() == null) {
                bestaatNog = true;
            } else {
                QName dbkolom = DataStoreUtil.convertFullnameToQName(tdi.getKolomnaam());

                for (String attribute : attributes) {
                    QName attributeName = DataStoreUtil.convertFullnameToQName(attribute);
                    if (attributeName.getLocalPart().compareTo(dbkolom.getLocalPart()) == 0) {
                        bestaatNog = true;
                        break;
                    }
                }
            }
            //als alles tot hier goed is dan nog controleren of het commando nog wel gebouwd kan worden.
            if (bestaatNog) {
                String commando = null;
                //alleen voor commando met een "[" er in
                if (tdi.getDataType().getId() == DataTypen.QUERY && tdi.getCommando() != null && tdi.getCommando().indexOf("[") >= 0) {
                    commando = tdi.getCommando();
                }
                if (commando != null) {
                    for (String attribute : attributes) {
                        if (commando.indexOf("[" + attribute + "]") != -1) {
                            commando = commando.replaceAll("\\[" + attribute + "\\]", "");
                        }
                        //als alle commando velden zijn gevonden dan breaken.
                        if (commando.indexOf("[") == -1) {
                            break;
                        }
                    }
                    if (commando != null && commando.indexOf("[") >= 0) {
                        bestaatNog = false;
                    }
                }
            } else {
                uglyThemaData.append("[");
                uglyThemaData.append(tdi.getId() + ":KOLOMNAAM");
                uglyThemaData.append("]");
            }
            if (!bestaatNog) {
                uglyThemaData.append("[");
                uglyThemaData.append(tdi.getId() + ":COMMANDO");
                uglyThemaData.append("]");
            }

            if (tdi.getDataorder() != null) {
                volgordeVelden.put(tdi.getId().toString(), tdi.getDataorder());
            }

            if (tdi.getLabel() != null) {
                labelVelden.put(tdi.getId().toString(), tdi.getLabel());
            }
        }

        dynaForm.set("volgordeVelden", volgordeVelden);
        dynaForm.set("labelVelden", labelVelden);

        request.setAttribute("listUglyThemaData", uglyThemaData);
        dynaForm.set("basisregels", basisregels.toArray(new Integer[basisregels.size()]));
        dynaForm.set("editables", editables.toArray(new Integer[editables.size()]));

    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ThemaData td = getThemaData(dynaForm, false);
        if (td == null) {
            Gegevensbron gb = getGegevensbron(dynaForm, false);
            if (gb == null) {
                gb = getFirstGegevensbron();
            }

            td = getFirstThemaData(gb);
        }

        populateThemaDataForm(td, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    public ActionForward change(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Gegevensbron gb = getGegevensbron(dynaForm, false);
        if (gb == null) {
            gb = getFirstGegevensbron();
        }
        ThemaData td = getFirstThemaData(gb);
        populateThemaDataForm(td, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward create(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Gegevensbron gb = getGegevensbron(dynaForm, false);
        if (gb == null) {
            gb = getFirstGegevensbron();
        }
        dynaForm.initialize(mapping);
        String val = "";
        if (gb != null) {
            val = Integer.toString(gb.getId());
        }
        dynaForm.set("gegevensbronID", val);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Gegevensbron gb = getGegevensbron(dynaForm, false);
        if (gb == null) {
            gb = getFirstGegevensbron();
        }
        ThemaData td = getThemaData(dynaForm, false);
        if (td == null) {
            td = getFirstThemaData(gb);
        }
        populateThemaDataForm(td, dynaForm, request);
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, VALIDATION_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        ThemaData t = getThemaData(dynaForm, true);
        if (t == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateThemaDataObject(dynaForm, t, request);

        sess.saveOrUpdate(t);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(t);
        populateThemaDataForm(t, dynaForm, request);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    public ActionForward updateBasisregels(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Integer[] basisregels = (Integer[]) dynaForm.get("basisregels");

        Gegevensbron gb = getGegevensbron(dynaForm, false);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<ThemaData> bestaandeObjecten = SpatialUtil.getThemaData(gb, false);
        for (ThemaData td : bestaandeObjecten) {
            boolean isBasis = false;
            for (Integer bs : basisregels) {
                if (bs != null && bs.compareTo(td.getId()) == 0) {
                    isBasis = true;
                    break;
                }
            }

            if (isBasis) {
                td.setBasisregel(true);
            } else {
                td.setBasisregel(false);
            }

            sess.saveOrUpdate(td);
            sess.flush();
        }

        saveVolgordeVelden(dynaForm);
        saveLabelVelden(dynaForm);

        return unspecified(mapping, dynaForm, request, response);
    }

    private void saveVolgordeVelden(DynaValidatorForm dynaForm) {
        Map volgordeVelden = (HashMap) dynaForm.get("volgordeVelden");

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Iterator it = volgordeVelden.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();

            Integer key = null;
            Integer volgorde = null;

            try {
                key = new Integer((String) pairs.getKey());
                volgorde = new Integer((String) pairs.getValue());
            } catch (NumberFormatException nfe) {
                logger.debug("Fout tijdens omzetten volgorde waardes van objectdata veld.");
            }

            if (key != null && key > 0 && volgorde != null) {
                ThemaData td = (ThemaData) sess.get(ThemaData.class, key);

                if (td != null) {
                    if (volgorde < 1) {
                        td.setDataorder(null);
                    } else if (volgorde > 0) {
                        td.setDataorder(new Integer(volgorde));
                    }

                    sess.saveOrUpdate(td);
                }
            }
        }

        sess.flush();
    }

    private void saveLabelVelden(DynaValidatorForm dynaForm) {
        Map labelVelden = (HashMap) dynaForm.get("labelVelden");

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Iterator it = labelVelden.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();

            Integer key = null;
            String label = (String) pairs.getValue();

            try {
                key = new Integer((String) pairs.getKey());
            } catch (NumberFormatException nfe) {
                logger.debug("Fout tijdens omzetten volgorde waardes van objectdata veld.");
            }

            if (key != null && key > 0 && label != null) {
                ThemaData td = (ThemaData) sess.get(ThemaData.class, key);

                if (td != null) {
                    if (label.equals("")) {
                        td.setLabel(null);
                    } else if (!label.equals("")) {
                        td.setLabel(label);
                    }

                    sess.saveOrUpdate(td);
                }
            }
        }

        sess.flush();
    }

    public ActionForward createAllThemaData(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Gegevensbron gb = getGegevensbron(dynaForm, false);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Bron b = gb.getBron(request);

        List<String> attributes = new ArrayList();
        try {
            attributes = DataStoreUtil.getAttributeNames(b, gb);
        } catch (SocketTimeoutException e) {
            logger.error("Socket time out error while getting attributes.");
        }

        /* Workaround voor ophalen objectdata voor WFS 110 */
        if (attributes == null || attributes.size() < 1) {
            attributes = getObjectDataForWFS110(b, gb);
        }

        if (attributes == null || attributes.size() < 1) {
            return unspecified(mapping, dynaForm, request, response);
        }

        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        Name geomName = null;
        try {
            geomName = DataStoreUtil.getThemaGeomName(gb, user);
        } catch (Exception ex) {
            logger.debug("", ex);
        }
        String geomPropname = "";
        if (geomName != null && geomName.getLocalPart() != null) {
            geomPropname = geomName.getLocalPart();
        }

        List<ThemaData> bestaandeObjecten = SpatialUtil.getThemaData(gb, false);
        for (String attribute : attributes) {
            QName attributeName = DataStoreUtil.convertFullnameToQName(attribute);
            if (attributeName == null || attributeName.getLocalPart().compareTo(geomPropname) == 0) {
                // geometry column not added
                continue;
            }
            boolean bestaatAl = false;
            for (ThemaData td : bestaandeObjecten) {
                if (td.getKolomnaam() == null) {
                    continue;
                }
                QName dbkolomName = DataStoreUtil.convertFullnameToQName(td.getKolomnaam());
                if (attributeName.getLocalPart().compareTo(dbkolomName.getLocalPart()) == 0) {
                    bestaatAl = true;
                    break;
                }
            }
            if (!bestaatAl) {
                ThemaData td = new ThemaData();
                td.setBasisregel(false);
                td.setDataType((DataTypen) sess.get(DataTypen.class, DataTypen.DATA));
                String netteNaam = attributeName.getLocalPart();
                if (netteNaam.indexOf("{") >= 0 && netteNaam.indexOf("}") >= 0) {
                    netteNaam = netteNaam.substring(netteNaam.indexOf("}") + 1);
                }
                td.setLabel(netteNaam);
                td.setKolomnaam(attributeName.getLocalPart());
                td.setGegevensbron(gb);
                td.setDefaultValues("");
                td.setEditable(false);
                td.setWaardeType((WaardeTypen) sess.get(WaardeTypen.class, WaardeTypen.STRING));
                sess.saveOrUpdate(td);
            } else {
                //niks doen
            }
        }
        // kijken of oude themadata verwijderd moet worden omdat bijbehorend attribuut niet meer bestaat
        // kijken of er een Extra data veld is of nog moet aangemaakt worden.
        boolean extraVeldBestaatAl = false;
        boolean erIsEenBasisRegel = false;

        for (ThemaData td : bestaandeObjecten) {
            if (td.isBasisregel()) {
                erIsEenBasisRegel = true;
            }
            if (td.getCommando() != null && td.getCommando().toLowerCase().startsWith("viewerdata.do?aanvullendeinfo=t")) {
                extraVeldBestaatAl = true;
            }
            if (td.getKolomnaam() == null) {
                continue;
            }

            QName dbkolom = DataStoreUtil.convertFullnameToQName(td.getKolomnaam());
            boolean bestaatNog = false;
            for (String attribute : attributes) {
                QName attributeName = DataStoreUtil.convertFullnameToQName(attribute);
                if (attributeName.getLocalPart().compareTo(dbkolom.getLocalPart()) == 0) {
                    bestaatNog = true;
                    break;
                }
            }

            if (!bestaatNog) {
                Gegevensbron tgb = td.getGegevensbron();
                tgb.getThemaData().remove(td);
                sess.delete(td);
                sess.flush();
            }
        }

        if (attributes.size() > DEFAULTBASISCOLUMNS) {
            if (!extraVeldBestaatAl) {
                ThemaData td = createDefaultExtraThemaData(gb);
                sess.saveOrUpdate(td);
            }
        }

        return unspecified(mapping, dynaForm, request, response);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ThemaData td = getThemaData(dynaForm, false);
        if (td == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Gegevensbron gb = td.getGegevensbron();
        gb.getThemaData().remove(td);
        sess.delete(td);
        sess.flush();

        td = getFirstThemaData(gb);
        dynaForm.initialize(mapping);
        populateThemaDataForm(td, dynaForm, request);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    private void populateThemaDataForm(ThemaData td, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (td == null) {
            return;
        }
        if (td.getId() != null) {
            dynaForm.set("themaDataID", Integer.toString(td.getId()));
        } else {
            dynaForm.set("themaDataID", "");
        }
        dynaForm.set("label", td.getLabel());
        dynaForm.set("eenheid", td.getEenheid());
        dynaForm.set("omschrijving", td.getOmschrijving());
        String val = "";
        if (td.getGegevensbron() != null) {
            val = Integer.toString(td.getGegevensbron().getId());
        }
        dynaForm.set("gegevensbronID", val);
        dynaForm.set("basisregel", td.isBasisregel());
        dynaForm.set("editable", td.isEditable());
        dynaForm.set("defaultValues", td.getDefaultValues());
        dynaForm.set("voorbeelden", td.getVoorbeelden());
        dynaForm.set("kolombreedte", FormUtils.IntToString(td.getKolombreedte()));
        val = "";
        if (td.getWaardeType() != null) {
            val = Integer.toString(td.getWaardeType().getId());
        }
        dynaForm.set("waardeTypeID", val);
        val = "1";
        if (td.getDataType() != null) {
            val = Integer.toString(td.getDataType().getId());
        }
        dynaForm.set("dataTypeID", val);
        dynaForm.set("commando", td.getCommando());
        dynaForm.set("kolomnaam", td.getKolomnaam());
        if (td.getDataorder() != null) {
            dynaForm.set("dataorder", FormUtils.IntToString(td.getDataorder()));
        }

    }

    private void populateThemaDataObject(DynaValidatorForm dynaForm, ThemaData td, HttpServletRequest request) {

        Boolean b = (Boolean) dynaForm.get("basisregel");
        td.setBasisregel(b == null ? false : b.booleanValue());
        td.setCommando(FormUtils.nullIfEmpty(dynaForm.getString("commando")));

        if (dynaForm.getString("dataorder") != null && dynaForm.getString("dataorder").length() > 0) {
            td.setDataorder(Integer.parseInt(dynaForm.getString("dataorder")));
        } else {
            td.setDataorder(null);
        }

        td.setEenheid(FormUtils.nullIfEmpty(dynaForm.getString("eenheid")));
        td.setKolombreedte(FormUtils.StringToInt(dynaForm.getString("kolombreedte")));
        td.setKolomnaam(FormUtils.nullIfEmpty(dynaForm.getString("kolomnaam")));
        td.setLabel(FormUtils.nullIfEmpty(dynaForm.getString("label")));
        td.setOmschrijving(FormUtils.nullIfEmpty(dynaForm.getString("omschrijving")));
        td.setVoorbeelden(FormUtils.nullIfEmpty(dynaForm.getString("voorbeelden")));
        Boolean editable = (Boolean) dynaForm.get("editable");
        td.setEditable(editable == null ? false : editable.booleanValue());

        String defaultValues = FormUtils.nullIfEmpty(dynaForm.getString("defaultValues"));
        if (defaultValues != null) {
            // Delete spaces between entries and comma's
            String[] entries = defaultValues.split(",");
            for (int i = 0; i < entries.length; i++) {
                entries[i] = entries[i].trim();
            }
            defaultValues = StringUtils.join(entries, ",");
        }
        td.setDefaultValues(defaultValues);


        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        int tId = 0, dId = 0, wId = 0;
        try {
            tId = Integer.parseInt(dynaForm.getString("gegevensbronID"));
        } catch (NumberFormatException ex) {
            logger.error("Illegal gegevensbronID", ex);
        }
        try {
            dId = Integer.parseInt(dynaForm.getString("dataTypeID"));
        } catch (NumberFormatException ex) {
            logger.error("Illegal dataTypeID", ex);
        }

        Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, new Integer(tId));
        td.setGegevensbron(gb);
        DataTypen d = (DataTypen) sess.get(DataTypen.class, new Integer(dId));
        td.setDataType(d);

    }

    protected ThemaData createDefaultExtraThemaData(Gegevensbron gb) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        ThemaData td = new ThemaData();
        td.setLabel("Extra");
        td.setBasisregel(false);
        td.setEditable(false);
        td.setDefaultValues(null);
        td.setKolombreedte(50);
        td.setWaardeType((WaardeTypen) sess.get(WaardeTypen.class, WaardeTypen.STRING));
        td.setDataType((DataTypen) sess.get(DataTypen.class, DataTypen.URL));
        td.setCommando("viewerdata.do?aanvullendeinfo=t&");
        td.setGegevensbron(gb);
        return td;
    }

    private List<String> getObjectDataForWFS110(Bron b, Gegevensbron gb) {
        List<String> attributes = new ArrayList<String>();

        try {
            List attrArr = ConfigListsUtil.getPossibleAttributes(b, gb.getAdmin_tabel());

            for (Object obj : attrArr) {
                if (obj != null && obj instanceof String[]) {
                    String[] values = (String[]) obj;
                    attributes.add(values[0]);
                }
            }
        } catch (Exception ex) {
            logger.error("Fout tijdens ophalen objectdata voor WFS 110: ", ex);
        }

        return attributes;
    }
}
