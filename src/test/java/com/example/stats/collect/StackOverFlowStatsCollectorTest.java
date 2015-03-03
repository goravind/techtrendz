package com.example.stats.collect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.example.application.ApplicationStart;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ApplicationStart.class)
@ActiveProfiles(profiles = "test")
@IntegrationTest
public class StackOverFlowStatsCollectorTest {

	@Autowired
	private GitHubStatsCollector stackoverflowcollector;

	@Test
	public void testCollect() throws Exception {
		stackoverflowcollector.collect();
	}

}
