import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class timestampComm extends Thread implements timestamp {

	@Override
	public boolean canDeliver(timestamp other) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int get(int id) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int id() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void increment() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void max(timestamp other) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

}
