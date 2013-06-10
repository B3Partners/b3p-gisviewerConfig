package nl.b3p.gis.viewer;

import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.viewer.db.Tekstblok;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

/**
 *
 * @author Boy de Wit
 */
public class ConfigCMSAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigCMSAction.class);

    private Tekstblok getTekstblok(DynaValidatorForm form, boolean createNew) {
        if (createNew) {
            return new Tekstblok();
        }

        Integer id = (Integer) form.get("tekstBlokID");

        if (id != null && id > 0) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            Tekstblok tb = (Tekstblok) sess.get(Tekstblok.class, id);

            if (tb != null)
                return tb;
        }

        return null;
    }

    private Tekstblok getFirstTekstblok() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List lijst = sess.createQuery("from Tekstblok order by cdate")
                .setMaxResults(1).list();

        if (lijst != null && lijst.size() == 1) {
            return (Tekstblok) lijst.get(0);
        }

        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        //super.createLists(form, request);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List tekstBlokken = sess.createQuery("from Tekstblok order by cdate").list();
        request.setAttribute("tekstBlokken", tekstBlokken);
        
        List cmsPaginas = sess.createQuery("from CMSPagina order by cdate").list();
        request.setAttribute("cmsPaginas", cmsPaginas);
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Tekstblok tb = getTekstblok(dynaForm, false);
        if (tb == null) {
            tb = getFirstTekstblok();
        }

        populateTekstblokForm(tb, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Tekstblok tb = getTekstblok(dynaForm, false);
        if (tb == null) {
            tb = getFirstTekstblok();
        }

        populateTekstblokForm(tb, dynaForm, request);

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

        Tekstblok tb = getTekstblok(dynaForm, true);

        if (tb == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        populateTekstblokObject(dynaForm, tb, request);

        sess.saveOrUpdate(tb);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(tb);
        populateTekstblokForm(tb, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        
        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* Probeer Tekstblok uit de database te verwijderen */
        Session sess = null;
        Tekstblok tb = null;

        try {
            sess = HibernateUtil.getSessionFactory().getCurrentSession();
            tb = getTekstblok(dynaForm, false);

            if (tb == null) {
                prepareMethod(dynaForm, request, LIST, EDIT);
                addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);

                return getAlternateForward(mapping, request);
            }

            sess.delete(tb);
            sess.flush();

        } catch (Exception ex) {
            logger.error("Fout tijdens verwijderen van tekstblok " + tb.getTitel(), ex);

            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        Tekstblok tekstBlok = getFirstTekstblok();

        if (tekstBlok != null)
            populateTekstblokForm(tekstBlok, dynaForm, request);
        else
            dynaForm.initialize(mapping);
        
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    private void populateTekstblokForm(Tekstblok tb, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (tb == null) {
            return;
        }

        dynaForm.set("tekstBlokID", tb.getId());
        dynaForm.set("titel", tb.getTitel());
        dynaForm.set("tekst", tb.getTekst());
        dynaForm.set("url", tb.getUrl());
        dynaForm.set("toonUrl", tb.getToonUrl());        
        dynaForm.set("volgordeNr", tb.getVolgordeNr());
        dynaForm.set("kleur", tb.getKleur());
        dynaForm.set("inlogIcon", tb.getInlogIcon());
        dynaForm.set("hoogte", tb.getHoogte());
        dynaForm.set("cmsPagina", tb.getCmsPagina());
    }

    private void populateTekstblokObject(DynaValidatorForm dynaForm, Tekstblok tb, HttpServletRequest request) {
        Integer id = (Integer) dynaForm.get("tekstBlokID");

        if (id != null && id != 0) {
            tb.setId(id);
        }

        tb.setTitel(FormUtils.nullIfEmpty(dynaForm.getString("titel")));
        tb.setTekst(FormUtils.nullIfEmpty(dynaForm.getString("tekst")));
        tb.setUrl(FormUtils.nullIfEmpty(dynaForm.getString("url")));
        tb.setToonUrl((Boolean) dynaForm.get("toonUrl"));        
        tb.setKleur(FormUtils.nullIfEmpty(dynaForm.getString("kleur")));
        tb.setInlogIcon((Boolean) dynaForm.get("inlogIcon"));
        
        Integer hoogte = (Integer)dynaForm.get("hoogte");
        if (hoogte != null){
            tb.setHoogte(hoogte);
        }
        

        Integer volgordeNr = (Integer) dynaForm.get("volgordeNr");

        if (volgordeNr != null) {
            tb.setVolgordeNr(volgordeNr);
        }  

        /* TODO: vervangen voor ingelogde gebruikersnaam */
        tb.setAuteur("beheerder");

        Date cdate = new Date();
        tb.setCdate(cdate);
        
        Integer cmsPagina = (Integer)dynaForm.get("cmsPagina");
        if (cmsPagina != null && cmsPagina > 0){
            tb.setCmsPagina(cmsPagina);
        } else {
            tb.setCmsPagina(null);
        }
    }
}
