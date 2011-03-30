package nl.b3p.gis.viewer;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Bron;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.apache.struts.validator.DynaValidatorForm;
import org.geotools.data.DataStore;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

/**
 *
 * @author B3Partners
 */
public class ConfigConnectieAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigConnectieAction.class);

    protected static final String FK_PARENTBRON_ERROR_KEY = "error.fk.parentbron";
    
    protected Bron getConnectie(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("bronId"));
        Bron c = null;
        if (id == null && createNew) {
            c = new Bron();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            c = (Bron) sess.get(Bron.class, id);
        }
        return c;
    }

    protected Bron getFirstConnectie() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List cs = sess.createQuery("from Bron order by naam").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Bron) cs.get(0);
        }
        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        super.createLists(form, request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List alleBronnen = sess.createQuery("from Bron order by naam").list();
        request.setAttribute("allConnecties", alleBronnen);

        /* lijstje werkende bronnen om status te kunnen plaatsen bij niet werkende */
        Iterator iter = alleBronnen.iterator();
        List validBronIds = new ArrayList();

        while (iter.hasNext()) {
            Bron b = (Bron) iter.next();
            DataStore ds = null;

            try {
                ds = b.toDatastore();

                if (ds != null)
                    validBronIds.add(b.getId());

            /* deze treed bijvoorbeeld op als je een bron bewerkt en hij kan de
             * GetCapabilities niet ophalen */
            } catch (FileNotFoundException ex) {
                logger.debug("Kon tijdens bewerken van bron " + b.getNaam() + " de GetCapabilities niet ophalen.");

            } catch (Exception e) {
                logger.error("Exception for bron: "+b.getId()+" URL: "+b.getUrl(), e);
            } finally {
                if (ds != null)
                    ds.dispose();
            }
        }

        request.setAttribute("validBronIds", validBronIds);
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Bron c = getConnectie(dynaForm, false);
        if (c == null) {
            c = getFirstConnectie();
        }
        populateConnectieForm(c, dynaForm, request);
        
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Bron c = getConnectie(dynaForm, false);
        if (c == null) {
            c = getFirstConnectie();
        }
        populateConnectieForm(c, dynaForm, request);
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

        Bron c = getConnectie(dynaForm, true);
        if (c == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateConnectieObject(dynaForm, c, request);

        sess.saveOrUpdate(c);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(c);
        populateConnectieForm(c, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* Probeer de Bron uit de database te verwijderen. Er treed een
         * ConstraintViolationException op indien hier nog een ZoekConfiguratie
         * aan is gekoppeld via een FK*/
        Session sess = null;
        Bron c = null;

        try {
            sess = HibernateUtil.getSessionFactory().getCurrentSession();
            c = getConnectie(dynaForm, false);

            if (c == null) {
                prepareMethod(dynaForm, request, EDIT, LIST);
                addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);

                return getAlternateForward(mapping, request);
            }

            sess.delete(c);
            sess.flush();

        } catch (ConstraintViolationException ex) {

            /* ophalen gekoppelde zoekconfiguraties */
            List zoekConfigs = sess.createQuery("from ZoekConfiguratie where "
                    + " parentbron = :bronid")
                    .setParameter("bronid", c.getId())
                    .list();

            String zcNamen = "";
            if (zoekConfigs != null && zoekConfigs.size() > 1) {
                Iterator iter = zoekConfigs.iterator();

                while (iter.hasNext()) {
                    ZoekConfiguratie zc = (ZoekConfiguratie) iter.next();

                    if (zc.getNaam() != null) {
                        if (zcNamen.length() < 1)
                            zcNamen += zc.getNaam();
                        else
                            zcNamen += ", "+zc.getNaam();

                    } else {
                        if (zcNamen.length() < 1)
                            zcNamen += zc.getFeatureType();
                        else
                            zcNamen += ", " +zc.getFeatureType();
                    }
                }
            }

            logger.error("Kon bron "+ c.getNaam() +" niet verwijderen. Er is nog een"
                    + " zoekconfiguratie aan gekoppeld.");

            MessageResources msg = getResources(request);
            Locale locale = getLocale(request);
            String melding = msg.getMessage(locale, FK_PARENTBRON_ERROR_KEY, zcNamen);

            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, null, melding);

            return getAlternateForward(mapping, request);

        } catch (Exception ex) {
            logger.error("", ex);
            
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        /* Eerstvolgende bron klaarzetten voor formulier */
        Bron bron = getConnectie(dynaForm, false);
        if (bron == null) {
            bron = getFirstConnectie();
        }
        populateConnectieForm(bron, dynaForm, request);

        //dynaForm.initialize(mapping);
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        
        return getDefaultForward(mapping, request);
    }

    private void populateConnectieForm(Bron c, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (c == null) {
            return;
        }
        dynaForm.set("bronId", Integer.toString(c.getId().intValue()));
        dynaForm.set("naam", c.getNaam());
        dynaForm.set("url", c.getUrl());
        dynaForm.set("gebruikersnaam", c.getGebruikersnaam());
        dynaForm.set("wachtwoord", c.getWachtwoord());
        if (c.getVolgorde()!=null)
            dynaForm.set("volgorde", c.getVolgorde().toString());
    }

    private void populateConnectieObject(DynaValidatorForm dynaForm, Bron c, HttpServletRequest request) {
        if (FormUtils.nullIfEmpty(dynaForm.getString("bronId"))!=null){
            c.setId(new Integer (dynaForm.getString("bronId")));
        }
        c.setNaam(FormUtils.nullIfEmpty(dynaForm.getString("naam")));
        c.setUrl(FormUtils.nullIfEmpty(dynaForm.getString("url")));
        c.setGebruikersnaam(FormUtils.nullIfEmpty(dynaForm.getString("gebruikersnaam")));
        c.setWachtwoord(FormUtils.nullIfEmpty(dynaForm.getString("wachtwoord")));
        if (FormUtils.nullIfEmpty(dynaForm.getString("volgorde"))!=null){
            c.setVolgorde(new Integer (dynaForm.getString("volgorde")));
        }
    }
}
