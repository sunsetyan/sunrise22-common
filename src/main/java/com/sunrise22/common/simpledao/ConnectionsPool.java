/**
 * 
 */
package com.sunrise22.common.simpledao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sunrise22.common.tool.ExceptionTool;

/**
 * 数据库连接池
 *
 */
public class ConnectionsPool {
	/** 一个可涂改的属性 */
	public static String logPrefix = Thread.currentThread().getName() + ": ";
	
	/** 线程安全的获取实例调用 */
	public synchronized static ConnectionsPool getInstance() {
		if (connPoolInstance == null)
			connPoolInstance = new ConnectionsPool();
		return connPoolInstance;
	}
	
	/** 私有化构造子 */
	private ConnectionsPool() {
		connectionsPool = new LinkedList<Connection>();
		releaseConnectionPool = new Hashtable<String, Object[]>();
		connUsetimes = new HashMap<String, Long>();
		initConfig();
		initPool();
	}
	
	/** 按照设定数量初始化数据库连接池连接 */
	private void initPool() {
		try {
			Class.forName(driver);
			for (int i = 0; i < initCount; i++) {
				try {
					connectionsPool.addLast(this.createConnection());
				} catch (SQLException e) {
					log.info(logPrefix + "建立数据库链接异常: " + i);
					ExceptionTool.printStackTrace(e);
				}
			}
		} catch (ClassNotFoundException e) {
			log.info(logPrefix + "获取驱动异常: " + driver);
			ExceptionTool.printStackTrace(e);
		}
		currActiveCount = connectionsPool.size();
	}
	
	/** 归还连接，将连接从已分配连接池删除，并加入未分配连接池，计算总共使用次数，超过最多次数，则放弃该连接 */
	public synchronized void giveBackConnection(Connection conn) {
		releaseConnectionPool.remove(conn.toString());
		
		long nowUserTime = connUsetimes.get(conn.toString());
		if (nowUserTime > new Date().getTime()) {
			connectionsPool.addLast(conn);
		} else {
			this.abandonConnection(conn);
		}
	}
	
	/** 丢弃连接，将连接从已分配连接池删除，关闭，并减少当前活动连接数量，从计算连接时间中删除 */
	public void abandonConnection(Connection conn) {
		releaseConnectionPool.remove(conn.toString());
		connUsetimes.remove(conn.toString());
		this.currActiveCount--;
	}
	
	/** 外部从链接池中获取链接的接口。 */
	public Connection getConnection() {
		Connection conn = null;
		while (true) {
			try {
				conn = this.getInsideConnection();
				if (!conn.isClosed()) {
					// 验证其有效，并且未被关闭 则返回该链接
					break;
				} else {// 放弃该链接 重新获取，并且将该链接扔掉。
					this.abandonConnection(conn);
				}
			} catch (SQLException e) {
				log.warn(logPrefix + "获取链接失败, " + this.sleeptime + "秒之后重连。。");
				try {
					TimeUnit.MILLISECONDS.sleep(this.sleeptime * 1000);
				} catch (InterruptedException e1) {}// 忽略
			} 
		}
		return conn;
	}

	/** 获取可用的链接 
	 * @throws SQLException 
	 */
	private synchronized Connection getInsideConnection() throws SQLException {
		log.debug(logPrefix + "current poolSize: " + this.connectionsPool.size() 
				+ " releaseCount: " + this.releaseConnectionPool.size() 
				+ " activeCount: " + this.currActiveCount);
		Connection returnConn = null;
		if (this.connectionsPool.size() > 0) {
			// 若当前待分配连接池中还有连接，直接获取
			returnConn = this.connectionsPool.removeFirst();
			releaseConnectionPool.put(returnConn.toString(), new Object[] {
					returnConn, new Date() });
			return returnConn;
		}
		if (this.currActiveCount < this.maxCount) {
			// 待分配连接池中无连接，并且没有超过最大值
			for (int i = 0; i < this.increCount && this.currActiveCount < this.maxCount; i++, this.currActiveCount++) {
				connectionsPool.addLast(this.createConnection());
			}
			return this.getInsideConnection();
		} else {
			// 当前连接数超过最大连接数，尝试释放已分配连接池中连接，若有释放，则从待分配连接池中获取，否则抛出异常并且等待别的线程使用完成
			boolean hasOverTime = false;
			String conName = null;
			Connection conn = null;
			Enumeration<String> eConnections = releaseConnectionPool.keys();
			for (; eConnections.hasMoreElements();) {
				conName = eConnections.nextElement();
				// 察看上面的分配方式
				conn = (Connection) releaseConnectionPool.get(conName)[0];
				Date date = (Date) releaseConnectionPool.get(conName)[1];
				Date currDate = new Date();
				long time = (currDate.getTime() - date.getTime()) / 1000;
				if (time > overtime && conn != null) {
					releaseConnectionPool.remove(conName);
					connectionsPool.addLast(conn);
					hasOverTime = true;
				}
			}
			conn = null;
			if (hasOverTime) {
				return this.getInsideConnection();
			} else {// 重连的机制在上层做了控制，这个异常也会被捕捉起来。
				throw new SQLException("数据库连接池已经达到最大连接数，无法提供连接");
			}
		}
	}

	/** 创建链接 */
	private Connection createConnection() throws SQLException {
		Connection conn = DriverManager.getConnection(url, user, password);
		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		// 超过5小时则不再分配，而每次执行肯定不会超过3小时。
		connUsetimes.put(conn.toString(), new Date().getTime() + (5 * 60 * 60 * 1000));
		return conn;
	}

	/** 获取基本配置 */
	private void initConfig() {
		this.driver = PoolConfig.getProps("driver");
		this.url = PoolConfig.getProps("url");
		this.user = PoolConfig.getProps("user");
		this.password = PoolConfig.getProps("password");

		if (PoolConfig.getIntegerProps("initCount") > 0)
			this.initCount = PoolConfig.getIntegerProps("initCount");
		if (PoolConfig.getIntegerProps("maxCount") > 0)
			this.maxCount = PoolConfig.getIntegerProps("maxCount");
		if (PoolConfig.getIntegerProps("increCount") > 0)
			this.increCount = PoolConfig.getIntegerProps("increCount");
		if (PoolConfig.getIntegerProps("overtime") > 0)
			this.overtime = PoolConfig.getIntegerProps("overtime");
		if (PoolConfig.getIntegerProps("sleeptime") > 0)
			this.sleeptime = PoolConfig.getIntegerProps("sleeptime");
	}
	
	/** 唯一实例 */
	private static ConnectionsPool connPoolInstance = null;
	
	/** 已分配连接池 */
	private Hashtable<String, Object[]> releaseConnectionPool;
	
	/** 待分配连接池 */
	private LinkedList<Connection> connectionsPool; 
	
	/** 连接使用次数记录 */
	private Map<String,Long> connUsetimes; 
	
	private String driver;
	private String url;
	private String user;
	private String password;
	
	/** 连接池中初始创建连接数 */
	private int initCount = 3;
	
	/** 连接池中允许创建的最大的连接数 */
	private int maxCount = 50;
	
	/** 请求连接数大于活动连接数时，允许请求用户创建的连接数(必须大于0) */
	private int increCount = 3;
	
	/** 当前活动连接数，包括已经从连接池中分配出去的连接和连接池中的可分配连接 */
	private int currActiveCount;
	
	/** 超时时间，单位(s)。超过时间就回收连接 */
	private int overtime = 60;
	
	/** 获取连接失败后的休眠时间。单位(s)*/
	private int sleeptime = 30;
	
	private static final Log log = LogFactory.getLog(ConnectionsPool.class);
	
	public static void main(String[] args) {
		ConnectionsPool.getInstance();
		System.out.println("获取链接池成功!" + ConnectionsPool.getInstance());
	}

	@Override
	public String toString() {
		return "ConnectionsPool [releaseConnectionPool="
				+ releaseConnectionPool.size() + ", connectionsPool="
				+ connectionsPool.size() + ", connUsetimes=" + connUsetimes
				+ ", driver=" + driver + ", url=" + url + ", user=" + user
				+ ", password=" + password + ", initCount=" + initCount
				+ ", maxCount=" + maxCount + ", increCount=" + increCount
				+ ", currActiveCount=" + currActiveCount + ", overtime="
				+ overtime + ", sleeptime=" + sleeptime + "]";
	}

}
