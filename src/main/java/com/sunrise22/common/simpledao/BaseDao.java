/**
 * 
 */
package com.sunrise22.common.simpledao;

import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 基本的数据库操作
 */
public class BaseDao {
	
	/** 一个可涂改的属性 */
	public static String logPrefix = Thread.currentThread().getName() + ": ";
	
	/**
	 * 更新或者保存
	 * @param strSql 
	 */
	public synchronized void save(String strSql) {
		log.debug(logPrefix + "execute sql : " + strSql);
		Connection conn = ConnectionsPool.getInstance().getConnection();
		//PreparedStatement stmt = null;
		// try {
		// conn.setAutoCommit(false);
		// stmt = conn.prepareStatement(strSql);
		// }
	}
	
	
	public BaseDao() {}
	
	private static final Log log = LogFactory.getLog(BaseDao.class);

}
