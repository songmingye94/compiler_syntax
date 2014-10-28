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
public class Production {
    public int num;
    public String left;
    public ArrayList<String> right;
    public ArrayList<String> select;
    public Production(int no){
        this.num = no;
        this.left = null;
        this.right = new ArrayList<>();
        this.select = new ArrayList<>();
    }
}
