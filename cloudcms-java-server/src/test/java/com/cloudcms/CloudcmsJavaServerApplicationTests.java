/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloudcms.controllers.AdminController;
import com.cloudcms.server.CloudcmsDriver;
import com.cloudcms.server.CmsDriverBranchNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CloudcmsJavaServerApplicationTests {
	
	@Autowired
	private AdminController controller;
	
	@Autowired
	private CloudcmsDriver driver;

	@Test
	public void contextLoads() {
		assertThat(controller).isNotNull();
	}

	@Test
	public void cloudCmsDriverInitialized() {
		assertThat(driver.getDriver()).isNotNull();
		assertThat(driver.getPlatform()).isNotNull();
		assertThat(driver.getApplication()).isNotNull();
		assertThat(driver.getProject()).isNotNull();
		assertThat(driver.getMaster()).isNotNull();
		assertThat(driver.getBranch()).isNotNull();
	}

	@Test
	public void cloudCmsDriverFetchNode() throws CmsDriverBranchNotFoundException {
		assertThat(driver.getNodeById("master", "root", CloudcmsDriver.IGNORE_CACHE)).isNotNull();
	}
}