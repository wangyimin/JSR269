# JSR269

@GetterSetter  
public class Person {  
    private String name;  
}  

C:\>javac -cp \<path of jsr269.jar\>;.\ Person.java  
注意:getter&setter for [name] has been processed.  

C:\> javap -p Person.class  
Compiled from "Person.java"  
public class Person {  
  private java.lang.String name;  
  public void setName(java.lang.String);  
  public java.lang.String getName();  
  public Person();  
}  
