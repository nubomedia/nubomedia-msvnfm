package org.project.openbaton.cli.model;

import org.project.openbaton.cli.util.PrintFormat;
import org.project.openbaton.catalogue.nfvo.VimInstance;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by tce on 27.08.15.
 */
public class PrintVimInstance extends PrintFormat{


    public static boolean isVimInstance(List<Object> ob) {
        return ob.get(0) instanceof VimInstance;
    }


    public static String printVimInstance(String comand, List<Object> object) throws IllegalAccessException {
        String result = "";

       
            addRow("\n");
            addRow("+-------------------------------------", "+-----------------------", "+-------------------", "+");
            addRow("| ID", "| TENANT", "| NAME", "|");
            addRow("+-------------------------------------", "+-----------------------", "+-------------------", "+");
            for (int i = 0; i < object.size(); i++) {
                VimInstance vims = (VimInstance) object.get(i);
                addRow("| " + vims.getId(), "| " + vims.getTenant(), "| " + vims.getName(), "|");
                addRow("|", "|", "|", "|");
            }
            addRow("+-------------------------------------", "+-----------------------", "+-------------------", "+");


            result = printer();



            return result;
        }

}
