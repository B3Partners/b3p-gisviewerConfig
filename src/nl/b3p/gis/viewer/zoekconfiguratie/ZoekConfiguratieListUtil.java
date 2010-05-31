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

import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Bron;
import nl.b3p.zoeker.configuratie.ResultaatAttribuut;
import nl.b3p.zoeker.configuratie.ZoekAttribuut;
import nl.b3p.zoeker.services.Zoeker;
import org.geotools.data.DataStore;
import org.hibernate.Session;

/*
 * @author Roy Braam
 */
public class ZoekConfiguratieListUtil{

    /**
     * Haalt de mogelijke featureTypeNames van een bron op.
     **/
    public static String[] getTypeNames(Bron bron) throws Exception {
        if (bron==null){
            throw new Exception("Bron kan niet worden gevonden");
        }
        DataStore ds=Zoeker.getDataStore(bron);
        if (ds==null){
            throw new Exception("Kan geen verbinding maken met bron");
        }
        return ds.getTypeNames();
    }
    /**
     * Haalt de mogelijke featureTypeNames van een bron op adhv het id van de bron
     **/
    public static String[] getTypeNamesById(Integer bronId) throws Exception{
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Bron bron = (Bron) sess.get(Bron.class,bronId);
        return getTypeNames(bron);
    }

    public static void removeZoekAttribuut(Integer id){
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        ZoekAttribuut za = (ZoekAttribuut) sess.get(ZoekAttribuut.class, id);
        za.getZoekConfiguratie().getZoekVelden().remove(za);
        sess.delete(za);
    }
    public static void removeResultaatAttribuut(Integer id){
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        ResultaatAttribuut ra = (ResultaatAttribuut) sess.get(ResultaatAttribuut.class, id);
        ra.getZoekConfiguratie().getResultaatVelden().remove(ra);
        sess.delete(ra);
    }
}