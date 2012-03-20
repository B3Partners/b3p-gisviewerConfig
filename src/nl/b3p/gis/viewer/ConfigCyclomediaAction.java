package nl.b3p.gis.viewer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.db.CyclomediaAccount;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;
import org.json.JSONException;
import sun.security.rsa.RSAPrivateCrtKeyImpl;

public class ConfigCyclomediaAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigCyclomediaAction.class);
    
    private final String CERT_TYPE = "PKCS12";
    private final String KEY_FORMAT = "PKCS#8";
    private final String SIG_ALGORITHM = "SHA1withRSA";
    private final String URL_ENCODING = "utf-8";

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();

        ExtendedMethodProperties crudProp = null;

        crudProp = new ExtendedMethodProperties(SAVE);
        crudProp.setDefaultForwardName(SUCCESS);
        crudProp.setDefaultMessageKey("message.savecyclomedia.success");
        crudProp.setAlternateForwardName(FAILURE);
        crudProp.setAlternateMessageKey("message.savecyclomedia.failed");
        map.put(SAVE, crudProp);

        return map;
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        /* Applicatie code ophalen */
        String appCode = (String) request.getParameter("appcode");

        /* Applicatieinstellingen ophalen en klaarzetten voor form */
        Map map = null;
        ConfigKeeper configKeeper = new ConfigKeeper();
        map = configKeeper.getConfigMap(appCode);

        /* TODO weer uncommenten */
        if (map.size() < 1) {
            ConfigKeeper.writeDefaultApplicatie(appCode);
            map = configKeeper.getConfigMap(appCode);
        }

        populateForm(dynaForm, request, map, appCode);

        populateForApplicatieHeader(request, appCode);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }
    
    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        //Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        //List zoekconfigs = sess.createQuery("from ZoekConfiguratie order by naam").list();
        //request.setAttribute("zoekConfigs", zoekconfigs);        
    }

    private void populateForApplicatieHeader(HttpServletRequest request, String appCode) {
        Applicatie app = KaartSelectieUtil.getApplicatie(appCode);

        if (app != null) {
            request.setAttribute("header_appnaam", app.getNaam());
        }
    }

    public void populateForm(DynaValidatorForm dynaForm, HttpServletRequest request, Map map, String appCode) {   
        /* Zet cyclomedia account instellingen */
        ConfigKeeper keeper = new ConfigKeeper();
        CyclomediaAccount cycloAccount = keeper.getCyclomediaAccount(appCode);
        
        if (cycloAccount != null) {
            dynaForm.set("cfg_cyclo_apikey", cycloAccount.getApiKey());
            dynaForm.set("cfg_cyclo_accountid", cycloAccount.getAccountId());
            dynaForm.set("cfg_cyclo_wachtwoord", cycloAccount.getWachtwoord());
        }
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String appCode = (String) dynaForm.get("appcode");

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return this.getAlternateForward(mapping, request);
        }
        
        /* Opslaan Cyclomedia accountgegevens */
        saveCyclomediaAccount(appCode, dynaForm, request);
        
        populateForApplicatieHeader(request, appCode);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }
    
    private void saveCyclomediaAccount(String appCode, DynaValidatorForm dynaForm,
            HttpServletRequest request) throws JSONException, Exception {
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        
        /* Gegevens van form */
        String apiKey = dynaForm.getString("cfg_cyclo_apikey");
        String accountId = dynaForm.getString("cfg_cyclo_accountid");
        String wachtwoord = dynaForm.getString("cfg_cyclo_wachtwoord");
        String privateBase64Key = dynaForm.getString("cfg_cyclo_privatebase64key");        
        FormFile tempFormFile = (FormFile) dynaForm.get("cfg_cyclo_keybestand");
        
        if (apiKey != null && apiKey.equals("")) {
            apiKey = null;
        }
        if (accountId != null && accountId.equals("")) {
            accountId = null;
        }
        if (wachtwoord != null && wachtwoord.equals("")) {
            wachtwoord = null;
        }
        if (privateBase64Key != null && privateBase64Key.equals("")) {
            privateBase64Key = null;
        }
        
        /* Indien bestand geupload dan key eruit halen */
        File keyFile = null;
        if (tempFormFile != null && !tempFormFile.getFileName().equals("")) {            
            keyFile = new File(tempFormFile.getFileName());
  
            OutputStream os = new FileOutputStream(keyFile);  
            InputStream is = new BufferedInputStream(tempFormFile.getInputStream());  
            int count;  
            byte buf[] = new byte[4096];  

            while ((count = is.read(buf)) > -1) {  
                os.write(buf, 0, count);    
            }  

            is.close(); 
        }
        
        /* Als er geen private key door beheerder is ingevuld dan die uit bestand 
         * gebruiken */
        if (keyFile != null) {
            String base64 = null;
            try {
                base64 = getBase64EncodedPrivateKeyFromPfxFile(keyFile, wachtwoord);
            } catch (IOException iox) {
                logger.error("Fout tijdens openen " + keyFile.getName() + " bestand. Wachtwoord verkeerd ?");
            }
            
            if (base64 != null && !base64.equals("")) {
                privateBase64Key = base64;
            }
        }
        
        /* Zijn er al instellingen voor deze applicatie ? */
        ConfigKeeper keeper = new ConfigKeeper();
        CyclomediaAccount cycloAccount = keeper.getCyclomediaAccount(appCode);
        
        /* Zo nee dan nieuwe aanmaken */
        if (cycloAccount == null) {
            cycloAccount = new CyclomediaAccount();
            cycloAccount.setApiKey(apiKey);
            cycloAccount.setAccountId(accountId);
            cycloAccount.setWachtwoord(wachtwoord);
            cycloAccount.setPrivateBase64Key(privateBase64Key);
            cycloAccount.setAppCode(appCode);  
            
            sess.save(cycloAccount);
        } else {
            if (apiKey != null && !apiKey.equals(cycloAccount.getApiKey()) ) {
                cycloAccount.setApiKey(apiKey);
            }            
            if (accountId != null && !accountId.equals(cycloAccount.getAccountId()) ) {
                cycloAccount.setAccountId(accountId);
            }            
            if (wachtwoord != null && !wachtwoord.equals(cycloAccount.getWachtwoord()) ) {
                cycloAccount.setWachtwoord(wachtwoord);
            }
            if (privateBase64Key != null && !privateBase64Key.equals(cycloAccount.getPrivateBase64Key()) ) {
                cycloAccount.setPrivateBase64Key(privateBase64Key);
            }
            
            sess.update(cycloAccount);
        }
    }
    
    private String getBase64EncodedPrivateKeyFromPfxFile(File bestand, String password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {
        
        String base64 = null;
        PrivateKey privateKey = null;        
        
        KeyStore ks = java.security.KeyStore.getInstance(CERT_TYPE);
        ks.load(new FileInputStream(bestand), password.toCharArray());            

        Enumeration<String> aliases = ks.aliases();  
        
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            Key key = ks.getKey(alias, password.toCharArray());
            String keyFormat = key.getFormat();

            if ( (key instanceof RSAPrivateCrtKeyImpl) && keyFormat.equals(KEY_FORMAT) ) {
                privateKey = (PrivateKey)key;
            }
        }
        
        if (privateKey != null) {
            Base64 encoder = new Base64();
            base64 = new String(encoder.encode(privateKey.getEncoded()));
        }
        
        return base64;
    }
}