import java.io.Serializable;

//this is a simple object that holds the name of the player and its timestamp value

public class vectorobj implements Serializable {

	private static final long serialVersionUID = 1L;
	private int timeval;
	protected String playername;
	
	//Constructor
	public vectorobj(int timevalinit, String playernameinit) {
		
		timeval = timevalinit;
		playername = playernameinit;
	}
	
	public vectorobj clone() {
		vectorobj newobj = new vectorobj(this.timeval, this.playername);
		return newobj;
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
