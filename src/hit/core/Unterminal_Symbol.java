/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hit.core;

import java.util.*;

/**
 *
 * @author songmingye
 */
public class Unterminal_Symbol {//非终结符类
    public String value;
    public ArrayList<String> first_Set;
    public ArrayList<String> follow_Set;
    public Unterminal_Symbol(String string){
        this.value = string;
        this.first_Set = new ArrayList();
        this.follow_Set = new ArrayList();
    }
}
