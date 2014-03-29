package com.bridge.westeroscraft;

//This is the main class of WesterosBrdige aka "Where the Magic Happens"

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
//mysql stuff
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//code gen
import java.security.SecureRandom;
import java.math.BigInteger;

public class Main extends JavaPlugin implements Listener {
	// SQL prep
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	// code vars
	private ResultSet resultSet = null;
	private SecureRandom random = new SecureRandom();
	public String pCode = null;
	public String pExe = null;
	public String mcUser = null;

	// log all the things
	public void log(String text) {
		this.getLogger().log(Level.INFO, text);
	}

	// on disable
	public void onDisable() {
		close();
		log("Westerosbridge has been disabled.");
	}

	// on enable
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		// Register all the events
		pm.registerEvents(this, this);
		log("Westerosbridge has successfully been enabled");
		this.getServer().broadcastMessage("WesterosBridge Activated");
		// create thread
		new SQLThread("Checker").start();
		// connect
		try {
			try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				log(e.toString());
			}
			connect = DriverManager
					.getConnection("jdbc:mysql://localhost/westerosbridge?"
							+ "user=root&password=Westeroscraft2013!");
		} catch (SQLException e) {
			log(e.toString());
		}
		 //end connect
		 //sched
		 Bukkit.getServer().getScheduler()
		 .scheduleSyncRepeatingTask(this, new Runnable() {
		 public void run() {
		 try {
		 log("Getting results from westeros db.");
		 doCheck();
		 } catch (Exception e) {
		// Throw error if broken
		 log("Can't connect for some reason:" + e.toString());
		 }
		 }
		 }, 20, 600);
	}

	// sql threading

	// checker command for debug
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("check")) {
			// check shiz manually
			try {
				doCheck();
				log("Force checking WesterosCraft MMO DB...");
			} catch (Exception e) {
				log(e.toString());
			}
			this.getServer().broadcastMessage("Force Done!");
		}
		if (cmd.getName().equalsIgnoreCase("code")) {
			sender.sendMessage("Getting code...");
			try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				log(e.toString());
			}
			// all is well lets set this user up with an account.
			pExe = sender.getName();
			checkUser(pExe);
		}
		return false;
	}

	public void checkUser(String pExe) {

		try {
			// statement
			statement = connect.createStatement();
			resultSet = statement
					.executeQuery("select mcUser from users WHERE mcUser ='"
							+ pExe + "'");
			writePlayerCheck(resultSet);
		} catch (SQLException e) {
			log(e.toString());
		}
	}

	public void doCheck() throws Exception {
		// Setup the connection with the DB
		try {
			// statement
			statement = connect.createStatement();
			// get the stuff
			resultSet = statement
					.executeQuery("select * from users WHERE valNeed = 1");
			writeResultSet(resultSet);
		} catch (SQLException e) {
			this.getServer().broadcastMessage(e.toString());

		} finally {
			this.getServer().broadcastMessage("Results done!");
		}
	}

	public void writeCode() throws SQLException {
		// genCode
		pCode = codeGen();
		// write
		preparedStatement = connect
				.prepareStatement("INSERT into users VALUES (default, ?, ?, ?, ?, ? , ?)");
		preparedStatement.setString(1, pExe);
		preparedStatement.setString(2, null);
		preparedStatement.setDate(3, null);
		preparedStatement.setString(4, null);
		preparedStatement.setString(5, pCode);
		preparedStatement.setString(6, "1");
		preparedStatement.executeUpdate();
	}

	public String codeGen() {
		return new BigInteger(130, random).toString(32);
	}

	private void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {
			this.getServer().broadcastMessage(e.toString());
		}
	}

	private void writeResultSet(ResultSet resultSet) throws SQLException {
		while (resultSet.next()) {
			String uid = resultSet.getString("uid");
			String mcUser = resultSet.getString("mcUser");
			String Mclass = resultSet.getString("class");
			String subClass = resultSet.getString("subClass");
			String house = resultSet.getString("house");
			String code = resultSet.getString("code");
			this.getServer().broadcastMessage(
					"Found: " + uid + " " + mcUser + ": " + Mclass + " / "
							+ subClass + " " + house + " with a code of "
							+ code);
		}
	}

	private void writePlayerCheck(ResultSet resultSet) throws SQLException {
		while (resultSet.next()) {
			String mcUser = resultSet.getString("mcUser");
			Player player = Bukkit.getPlayer(pExe);
			// check the if it
			if (mcUser != null) {
				player.sendMessage(ChatColor.RED
						+ "Sorry: That user already registered!");
				Bukkit.broadcastMessage("nulled:" + mcUser);
			}
			if (mcUser == null) {
				Bukkit.broadcastMessage("else:" + mcUser);
				writeCode();
				player.sendMessage("Your code is: http://git.westeroscraft.com/index.php?code="
						+ pCode);
			}
		}
	}

	class SQLThread extends Thread {
		public SQLThread(String dothis) {
			super(dothis);
		}
	}
}