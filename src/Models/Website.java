package Models;

import java.util.ArrayList;
import java.util.Random;

public class Website {
	
	public String name;
	public int alltraffic;
	public int orgtraffic;
	public int costToAttack;
	public int costToAlter;
	
	public static int WebsiteNumber = 1;
	public int id = WebsiteNumber;
	
	public Website(){
		generateRandom();
		WebsiteNumber++;
	}
	
	public Website(long seed){
		generateRandom(seed);
		WebsiteNumber++;
	}
	
	public Website(long seed, boolean bad){
		if(bad) {
			generateRandomBad(seed);
		}
		else {
			generateRandomGood(seed);
		}
		WebsiteNumber++;
	}
	
	public Website(int alltraffic, int orgtraffic, int costToAttack, int costToAlter){
		name = "Website "+id;
		this.alltraffic = alltraffic;
		this.orgtraffic = orgtraffic;
		this.costToAttack = costToAttack;
		this.costToAlter = costToAlter;
		WebsiteNumber++;
	}
	
	private void generateRandom(){
		this.name = "Website"+WebsiteNumber;
		
		Random r = new Random();
		
		//All traffic in-between 350 and 750
//		alltraffic = r.nextInt(401)+350;
		alltraffic = r.nextInt(41)+35;
		//Org Traffic in-between 50 and 100
//		orgtraffic = r.nextInt(51)+50;
		orgtraffic = r.nextInt(6)+5;
		//cost to attack in-between 40-49
//		costToAttack = r.nextInt(10)+40;
		costToAttack = r.nextInt(8)+2;
		//cost to alter in-between 40-49
		costToAlter = r.nextInt(2)+1;		
		

	}
	
	private void generateRandom(long seed){
		this.name = "Website"+WebsiteNumber;
		
		Random r = new Random(seed);
		
		//All traffic in-between 350 and 750
		alltraffic = r.nextInt(401)+350;
//		alltraffic = r.nextInt(41)+35;	
		//Org Traffic in-between 50 and 100
		orgtraffic = r.nextInt(51)+50;
//		orgtraffic = r.nextInt(6)+5;
		//cost to attack in-between 30-54
		costToAttack = r.nextInt(25)+30;
//		costToAttack = r.nextInt(8)+2;
		//cost to alter in-between 40-49
		costToAlter = r.nextInt(4)+1;
//		costToAlter = 1;
//		orgtraffic = r.nextInt(101)+1;
		// added by Ryan, try origami
//		costToAttack = 1;
////		costToAlter = 1;
//		costToAlter = r.nextInt(6)+1;
//		orgtraffic = 75;
	}
	
	private void generateRandomGood(long seed){
		this.name = "Website"+WebsiteNumber;
		
		Random r = new Random(seed);
		
		//All traffic in-between 350 and 750
//		alltraffic = r.nextInt(401)+350;
		alltraffic = r.nextInt(20)+41;
		//Org Traffic in-between 50 and 100
//		orgtraffic = r.nextInt(51)+50;
		orgtraffic = r.nextInt(5)+35;
		//cost to attack in-between 40-49
//		costToAttack = r.nextInt(10)+40;
		costToAttack = r.nextInt(4)+2;
		//cost to alter in-between 40-49
		costToAlter = r.nextInt(2)+1;	
		
		costToAttack = 3;
	}
	
	private void generateRandomBad(long seed){
		this.name = "Website"+WebsiteNumber;
		
		Random r = new Random(seed);
		
		//All traffic in-between 350 and 750
		alltraffic = (int) (r.nextInt(201)+150)/5;
//		alltraffic = r.nextInt(41)+35;	
		//Org Traffic in-between 50 and 100
		orgtraffic = (int) (r.nextInt(10)+10)/5;
//		orgtraffic = r.nextInt(6)+5;
		//cost to attack in-between 30-54
		costToAttack = r.nextInt(50)+300;
//		costToAttack = r.nextInt(8)+2;
		//cost to alter in-between 40-49
		costToAlter = r.nextInt(4)+1;	
		
		costToAttack = 3;
	}
	
	public String toString(){
		return name;//+" all: "+alltraffic+" org: "+orgtraffic+" cost: "+costToAttack+" alter: "+costToAlter;
	}
	
	public String toStringFull(){
		return name+" all: "+alltraffic+" org: "+orgtraffic+" cost: "+costToAttack+" alter: "+costToAlter;
	}

}
