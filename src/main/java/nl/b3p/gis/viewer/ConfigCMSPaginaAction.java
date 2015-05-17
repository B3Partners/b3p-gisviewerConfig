package nl.b3p.gis.viewer;

import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import static nl.b3p.gis.viewer.ViewerCrudAction.ACKNOWLEDGE_MESSAGES;
import nl.b3p.gis.viewer.db.CMSPagina;
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
public class ConfigCMSPaginaAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigCMSPaginaAction.class);
    
    private CMSPagina getCMSPagina(DynaValidatorForm form, boolean createNew) {
        if (createNew) {
            return new CMSPagina();
        }

        Integer id = (Integer) form.get("cmsPaginaID");

        if (id != null && id > 0) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            CMSPagina cmsPag = (CMSPagina) sess.get(CMSPagina.class, id);

            if (cmsPag != null) {
                return cmsPag;
            }
        }

        return null;
    }

    private CMSPagina getFirstCMSPagina() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List lijst = sess.createQuery("from CMSPagina order by cdate")
                .setMaxResults(1).list();

        if (lijst != null && lijst.size() == 1) {
            return (CMSPagina) lijst.get(0);
        }

        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        //super.createLists(form, request);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List cmsPaginas = sess.createQuery("from CMSPagina order by cdate").list();
        request.setAttribute("cmsPaginas", cmsPaginas);
        
        List cmsMenus = sess.createQuery("from CMSMenu order by titel").list();
        request.setAttribute("cmsMenus", cmsMenus);
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CMSPagina cmsPag = getCMSPagina(dynaForm, false);
        
        if (cmsPag == null) {
            cmsPag = getFirstCMSPagina();
        }

        populateCMSPaginaForm(cmsPag, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CMSPagina cmsPag = getCMSPagina(dynaForm, false);
        if (cmsPag == null) {
            cmsPag = getFirstCMSPagina();
        }

        populateCMSPaginaForm(cmsPag, dynaForm, request);

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

        CMSPagina cmsPag = getCMSPagina(dynaForm, true);

        if (cmsPag == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        populateCMSPaginaObject(dynaForm, cmsPag, request);

        sess.saveOrUpdate(cmsPag);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(cmsPag);
        populateCMSPaginaForm(cmsPag, dynaForm, request);

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
        CMSPagina cmsPag = null;

        try {
            sess = HibernateUtil.getSessionFactory().getCurrentSession();
            cmsPag = getCMSPagina(dynaForm, false);

            if (cmsPag == null) {
                prepareMethod(dynaForm, request, LIST, EDIT);
                addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);

                return getAlternateForward(mapping, request);
            }

            sess.delete(cmsPag);
            sess.flush();

        } catch (Exception ex) {
            logger.error("Fout tijdens verwijderen van cms pagina " + cmsPag.getTitel(), ex);

            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY);

            return getAlternateForward(mapping, request);
        }

        CMSPagina pag = getFirstCMSPagina();

        if (pag != null) {
            populateCMSPaginaForm(pag, dynaForm, request);
        } else {
            dynaForm.initialize(mapping);
        }
        
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    private void populateCMSPaginaForm(CMSPagina cmsPag, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (cmsPag == null) {
            return;
        }

        dynaForm.set("cmsPaginaID", cmsPag.getId());
        dynaForm.set("titel", cmsPag.getTitel());
        dynaForm.set("tekst", cmsPag.getTekst());
        dynaForm.set("thema", cmsPag.getThema());
        dynaForm.set("showPlainAndMapButton", cmsPag.getShowPlainAndMapButton());
        dynaForm.set("cmsMenu", cmsPag.getCmsMenu());        
        dynaForm.set("loginRequired", cmsPag.getLoginRequired());
    }

    private void populateCMSPaginaObject(DynaValidatorForm dynaForm, CMSPagina cmsPag, HttpServletRequest request) {
        Integer id = (Integer) dynaForm.get("cmsPaginaID");

        if (id != null && id != 0) {
            cmsPag.setId(id);
        }

        cmsPag.setTitel(FormUtils.nullIfEmpty(dynaForm.getString("titel")));
        cmsPag.setTekst(FormUtils.nullIfEmpty(dynaForm.getString("tekst")));
        cmsPag.setThema(FormUtils.nullIfEmpty(dynaForm.getString("thema")));
        
        Boolean showPlainAndMapButton = (Boolean) dynaForm.get("showPlainAndMapButton");
        
        if (showPlainAndMapButton != null && showPlainAndMapButton) {
            cmsPag.setShowPlainAndMapButton(true);
        } else {
            cmsPag.setShowPlainAndMapButton(false);
        }        
        
        cmsPag.setCdate(new Date());
        
        Integer cmsMenu = (Integer)dynaForm.get("cmsMenu");
        if (cmsMenu != null && cmsMenu > 0){
            cmsPag.setCmsMenu(cmsMenu);
        } else {
            cmsPag.setCmsMenu(null);
        }
        
        Boolean loginRequired = (Boolean) dynaForm.get("loginRequired");        
        if (loginRequired != null && loginRequired) {
            cmsPag.setLoginRequired(true);
        } else {
            cmsPag.setLoginRequired(false);
        } 
    }
}