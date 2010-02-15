import java.lang.*;
//this is a simple object that holds the name of the player and its timestamp value

public class vectorobj {
	
	public int timeval;
	protected String playername;
	
	//Constructor
	public vectorobj(int timevalinit, String playernameinit) {
		
		timeval = timevalinit;
		playername = playernameinit;
	}
	
	public void settime(int newtime){
		timeval = newtime;
	}
	
	public int gettime(){
		return timeval;
	}
	
	public String getplayer(){
		return playername;
	}
}
