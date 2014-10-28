/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hit.core;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author songmingye
 */
public class Syntax {//句法分析核心类
    public void Syntax_Analysis(ArrayList<Token> token_list){//句法分析核心函数
        if(derived_strings.empty()){
            explaination = "结束";
        }else{
            String top_ofinput;//输入缓冲区首字符
            if(token_list.isEmpty()){
                top_ofinput = "$";
            }else{
                Token t = token_list.get(0);
                top_ofinput = Token_Code[t.code];
            }
            String top_ofstack = derived_strings.peek();
            if(isTerminal(top_ofstack)){ //推导符号串栈顶为终结符
                if(top_ofinput.equals(top_ofstack)){//匹配相约
                    token_list.remove(0);
                    derived_strings.pop();
                    explaination = "终结符归约";
                }else{
                    derived_strings.pop();//不匹配弹出栈顶的终结符
                    explaination = "错误：栈顶终结符与输入符号不匹配";
                }
            }else if(isUnterminal(top_ofstack)){ //栈顶为非终结符
                //System.out.println(top_ofstack);
                if(!(predict_map.get(top_ofstack).containsKey(top_ofinput))){//（栈顶非终结符——输入元素）项为空
                    if(!token_list.isEmpty()){
                        token_list.remove(0);
                        explaination = "错误：忽略输入符号"+top_ofinput;
                    }else{
                        derived_strings.pop();
                        explaination = "错误：弹出栈顶元素";
                    }
                }else{
                    Production production = predict_map.get(top_ofstack).get(top_ofinput);
                    if(production.num==-1){//同步记号
                        derived_strings.pop();
                        explaination = "错误：同步记号，弹出栈顶非终结符";
                    }else{
                        derived_strings.pop();
                        for(int i=production.right.size()-1;i>=0;i--){
                            String sp = production.right.get(i);
                            //System.out.println(sp);
                            if(!"<NULL>".equals(sp)){
                                derived_strings.push(sp);
                            }
                        }
                        explaination = production.left+"->";
                        for(String s : production.right){
                            explaination += s;
                        }
                    }
                }
            }
        }
    }
    public void create_list(){try {
        //构造基础表
        FileReader fr=new FileReader("/Users/songmingye/Documents/compiler/Syntax/Song_Syntax/Syntax_rule.txt");//文法文件地址请根据自己的系统更换
        BufferedReader br=new BufferedReader(fr);
        String s;
        while((s = br.readLine())!=null){
            String b1[] = s.split("->");
            Production p = new Production(production_list.size());
            p.left = b1[0];
            String b2[] = b1[1].split("@");
            for(String sp : b2){
                p.right.add(sp);
                if(Character.isLowerCase(sp.charAt(1))){//非终结符小写
                    if(!isUnterminal(sp)){
                        Unterminal_Symbol new_unterminal = new Unterminal_Symbol(sp);
                        unterminal_list.add(new_unterminal);
                    }
                }else{
                    if(!"<NULL>".equals(sp)){
                        if(!isTerminal(sp)){
                            Terminal_Symbol new_terminal = new Terminal_Symbol(sp);
                            terminal_list.add(new_terminal);
                        }
                    }
                }
            }
            production_list.add(p);
            if(!isUnterminal(b1[0])){
                Unterminal_Symbol new_unterminal = new Unterminal_Symbol(b1[0]);
                unterminal_list.add(new_unterminal);
            }
        }
        } catch (IOException ex) {
            Logger.getLogger(Syntax.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void create_predict_map(){//构造预测分析表
        for(Production production : production_list){
            if(predict_map.containsKey(production.left)){
                HashMap temp = predict_map.get(production.left);
                for(String s : production.select){
                    temp.put(s, production);
                }
                predict_map.remove(production.left);
                predict_map.put(production.left, temp);
            }else{
                HashMap<String,Production> temp = new HashMap<>();
                for(String s : production.select){
                    temp.put(s, production);
                }
                predict_map.put(production.left, temp);
            }
        }
        create_synch();
    }
    public void create_synch(){//构造同步记号
        for(Unterminal_Symbol temp : unterminal_list){
            HashMap map = predict_map.get(temp.value);
            for(String s : temp.follow_Set){
                if(!map.containsKey(s)){
                    Production p = new Production(-1);//synch同步记号
                    map.put(s,p);
                }
            }
            predict_map.remove(temp.value);
            predict_map.put(temp.value, map);
        }
    }
    public void create_select(){//构造select集
        for(int i=0;i<production_list.size();i++){
            Production temp = production_list.get(i);
            if("<NULL>".equals(temp.right.get(0))){
                for(Unterminal_Symbol A : unterminal_list){
                    if(A.value.equals(temp.left)){
                        temp.select = (ArrayList<String>) A.follow_Set.clone();
                    }
                }
            }else{
                ArrayList<String> first_alpha = get_first_ofsequence(temp.right);
                if(can_beNullsequence(temp.right)){
                    for(Unterminal_Symbol A : unterminal_list){
                        if(A.value.equals(temp.left)){
                            temp.select = union_List(first_alpha,A.follow_Set);
                        }
                    }
                }else{
                    temp.select = (ArrayList<String>) first_alpha.clone();
                }
            }
            production_list.set(i, temp);
        }
        for(int i=0;i<production_list.size();i++){//人工矫正
            Production temp = production_list.get(i);
            if("<stmts'>".equals(temp.left) && "<NULL>".equals(temp.right.get(0))){
                temp.select.clear();
                temp.select.add("<}>");
            }
            production_list.set(i, temp);
        }
    }
    public void create_first(){//构造first集
        set_terminal_first();
        set_unterminal_first();
    }
    public void set_terminal_first(){//初始化first集
        for(int i=0;i<terminal_list.size();i++){
            Terminal_Symbol temp = terminal_list.get(i);
            temp.first_Set.add(temp.value);   //终结符FIRST(X)={X}
            terminal_list.set(i, temp);
        }
    }
    public void set_unterminal_first(){ //构造非终止符first集
        boolean havechanged;
        do{
            havechanged = false;
            for(int i=0;i<unterminal_list.size();i++){
                Unterminal_Symbol temp = unterminal_list.get(i);
                int cur_num = temp.first_Set.size();
                for(Production p : production_list){
                    if(temp.value.equals(p.left)){
                        for (String current_symbol : p.right) {
                            if(can_beNull(current_symbol)){
                                for(Unterminal_Symbol u : unterminal_list){
                                    if(u.value.equals(current_symbol)){
                                        temp.first_Set = union_List(temp.first_Set,u.first_Set);
                                    }
                                }
                            }else{
                                if(isTerminal(current_symbol)){
                                    if(!temp.first_Set.contains(current_symbol)){
                                        temp.first_Set.add(current_symbol);
                                    }
                                }else{
                                    for(Unterminal_Symbol u : unterminal_list){
                                        if(u.value.equals(current_symbol)){
                                            temp.first_Set = union_List(temp.first_Set,u.first_Set);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
                int next_num = temp.first_Set.size();
                if(next_num>cur_num){
                    havechanged = true;
                    unterminal_list.set(i, temp);
                }
            }
        }while(havechanged);
    }
    public void create_follow(){//构造follow集
        init_follow();
        boolean havechanged;
        do{
            havechanged = false;
            for(Production production : production_list){
                String currentLeft = production.left;
                ArrayList<String> currentRight = production.right;
                int length = currentRight.size();
                for(int i=0;i<length;i++){
                    String currentString = currentRight.get(i);
                    if(!isUnterminal(currentString)){//直到非终结符才计算follow集
                        continue;
                    }
                    if(i < length-1){
                        ArrayList<String> rest = new ArrayList<>(currentRight.subList(i+1, length));
                        ArrayList<String> firstSetofRest = get_first_ofsequence(rest);
                        for(int j=0;j<unterminal_list.size();j++){
                            if(unterminal_list.get(j).value.equals(currentString)){
                                Unterminal_Symbol unterminal = unterminal_list.get(j);
                                int len1 = unterminal.follow_Set.size();
                                unterminal.follow_Set = union_List(unterminal.follow_Set,firstSetofRest);
                                int len2 = unterminal.follow_Set.size();
                                if(len2>len1){
                                    havechanged = true;
                                    unterminal_list.set(j, unterminal);
                                }
                            }
                        }
                        if(can_beNullsequence(rest)){
                            for(int j=0;j<unterminal_list.size();j++){
                                if(unterminal_list.get(j).value.equals(currentString)){
                                    Unterminal_Symbol unterminal = unterminal_list.get(j);
                                    int len1 = unterminal.follow_Set.size();
                                    for(Unterminal_Symbol A : unterminal_list){
                                        if(A.value.equals(currentLeft)){
                                            unterminal.follow_Set = union_List(unterminal.follow_Set,A.follow_Set);
                                        }
                                    }
                                    int len2 = unterminal.follow_Set.size();
                                    if(len2>len1){
                                        havechanged = true;
                                        unterminal_list.set(j, unterminal);                                       
                                    }
                                }
                            }
                        }
                    }else{
                        for(int j=0;j<unterminal_list.size();j++){
                            if(unterminal_list.get(j).value.equals(currentString)){
                                Unterminal_Symbol unterminal = unterminal_list.get(j);
                                int len1 = unterminal.follow_Set.size();
                                for(Unterminal_Symbol A : unterminal_list){
                                    if(A.value.equals(currentLeft)){
                                        unterminal.follow_Set = union_List(unterminal.follow_Set,A.follow_Set);
                                    }
                                }
                                int len2 = unterminal.follow_Set.size();
                                if(len2>len1){
                                    havechanged = true;
                                    unterminal_list.set(j, unterminal);
                                }
                            }
                        }
                    }
                }
            }
        }while(havechanged);
    }
    public void init_follow(){//follow集的初始化
        for(int i=0;i<unterminal_list.size();i++){
            Unterminal_Symbol temp = unterminal_list.get(i);
            if("<s>".equals(temp.value)){
                temp.follow_Set.add("$");
                unterminal_list.set(i, temp);
            }
        }
    }
    public boolean can_beNullsequence(ArrayList<String> alpha){//文法串能否为空
        for(String s : alpha){
            if(!can_beNull(s)){
                return false;
            }
        }
        return true;
    }
    public ArrayList<String> get_first_ofsequence(ArrayList<String> alpha){//求语法串的first集
        ArrayList<String> first_set = new ArrayList<String>();
        for(String s : alpha){
            if(can_beNull(s)){
                for(Unterminal_Symbol temp : unterminal_list){
                    if(s.equals(temp.value)){
                        first_set = union_List(first_set,temp.first_Set);
                    }
                }
            }else{
                if(isTerminal(s)){
                    if(!first_set.contains(s)){
                        first_set.add(s);
                    }
                }else{
                    for(Unterminal_Symbol temp : unterminal_list){
                        if(s.equals(temp.value)){
                            first_set = union_List(first_set,temp.first_Set);
                        }
                    }
                }
                break;
            }
        }
        return first_set;
    }
    public ArrayList union_List(ArrayList a,ArrayList b){//集合并运算
        ArrayList c;
        c = (ArrayList) a.clone();
        for (Object temp : b) {
            if(!(c.contains(temp))){
                c.add(temp);
            }
        }
        return c;
    }
    public boolean isTerminal(String X){ //判断是否为终结符
        for (Terminal_Symbol temp : terminal_list) {
            if(temp.value.equals(X)){
                return true;
            }
        }
        return false;
    }
    public boolean isUnterminal(String X){
        for(Unterminal_Symbol temp : unterminal_list){
            if(temp.value.equals(X)){
                return true;
            }
        }
        return false;
    }
    public boolean can_beNull(String X){ //判断X可否推出空串
        if(isTerminal(X)){//X是终结符，则X不可能推出空串
            return false;
        }else{//X是非终结符
            if(canbeNull_list.contains(X) || X.equals("<NULL>")){
                return true;
            }
            for(Production p : production_list){
                if(X.equals(p.left)){
                    if("<NULL>".equals(p.right)){
                        canbeNull_list.add(X);
                        return true;
                    }else{
                        boolean flag = true;
                        for(int i=0;i<p.right.size();i++){
                            if(!can_beNull(p.right.get(i))){
                                flag = false;
                                break;
                            }
                        }
                        if(flag){
                            canbeNull_list.add(X);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
    public Syntax(){
        this.Token_Code_List = Arrays.asList(Token_Code);
        this.predict_map = new HashMap<>();
        this.terminal_list = new ArrayList<>();
        this.unterminal_list = new ArrayList<>();
        this.production_list = new ArrayList<>();
        this.canbeNull_list = new ArrayList<>();
        this.derived_strings = new Stack<>();
        this.explaination = null;
        //derived_strings.push("<s>");
    }
    
    public ArrayList<Terminal_Symbol> terminal_list; //终结符表
    public ArrayList<Unterminal_Symbol> unterminal_list; //非终结符表
    public ArrayList<Production> production_list; //表达式
    public ArrayList<String> canbeNull_list; //可推导为空符号表
    public HashMap<String,HashMap<String,Production>> predict_map;//预测分析表
    
    public Stack<String> derived_strings;//推导符号串
    public String explaination; //句法分析说明
    
    
    /*******词法种别码表******终结符表********/
    public static  String[] Token_Code = {"<IF>","<ELSE>","<FOR>","<DO>","<WHILE>","<RETURN>",
		"<INT>","<FLOAT>","<CHAR>","<DOUBLE>","<BOOLEAN>","<VOID>","<TRUE>","<FALSE>","<INCLUDE>","<STRING>","<<>","<>>","<=>","<<=>","<>=>","<<>>","<==>",
		"<*>","<\\>","<+>","< - >","</>","<;>","<!>","<CHARACTER>","<TEXT>","<ID>","<CONST_INT>",
		"<CONST_REAL>","<(>","<)>","<{>","<}>","<&>","<|>","<~>","<.>","<#>","<++>","< -- >","<%>","<,>"};//token种别码表
    public static List<String> Token_Code_List;
} 