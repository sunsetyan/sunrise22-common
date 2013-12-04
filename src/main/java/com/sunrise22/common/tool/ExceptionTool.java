/**
 * 
 */
package com.sunrise22.common.tool;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 关于异常的处理程序
 * 
 */
public class ExceptionTool {

	private static final Log log = LogFactory.getLog(ExceptionTool.class);

	public static void printStackTrace(Throwable e) {
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		log.error(Thread.currentThread() + ": " + e.getMessage() + "\t"
				+ errors.toString());
	}
	
}
