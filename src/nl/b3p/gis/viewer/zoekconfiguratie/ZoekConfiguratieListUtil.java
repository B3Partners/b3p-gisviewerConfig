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
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Bron;
import nl.b3p.zoeker.services.Zoeker;
import org.geotools.data.DataStore;
import org.hibernate.Session;
import org.hibernate.Transaction;

/*
 * @author Roy Braam
 */
public class ZoekConfiguratieListUtil{

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
    /*
    public static String[] getTypeNames(Integer bronId) throws Exception{
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Bron bron=null;
        Transaction t=null;
        try {
            t=sess.beginTransaction();
            bron = (Bron) sess.get(Bron.class,bronId);
            t.commit();
        }finally {
            sess.close();
        }
        return getTypeNames(bron);
    }*/
}