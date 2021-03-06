package com.maiya.task.restrict;

import com.maiya.common.enums.AuthorStatusEnum;
import com.maiya.common.enums.CrawlTaskStatusEnum;
import com.maiya.crawling.service.JDService;
import com.maiya.dal.dao.credit.MemberIntenetInfoDao;
import com.maiya.dal.dao.myCrawler.CrawlB2CRetryTaskDao;
import com.maiya.dal.model.CrawlB2CRetryTask;
import com.maiya.dal.model.MemberIntenetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 
 * @author xiangdf
 *
 */
@Service("JDCrawlTaskService")
public class JDCrawlTaskService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JDCrawlTaskService.class);

	@Autowired
	private CrawlB2CRetryTaskDao crawlB2CRetryTaskDao;

	@Autowired
	private MemberIntenetInfoDao memberIntenetInfoDao;

	@Autowired
	private JDService service;

	@Value("${b2c.crawl.fail.task.retry.num}")
	private int maxRetryNum;

	/**
	 * 失败的任务重试
	 */
	public void retryCrawl() {
		LOGGER.info("重试京东失败的任务 retryCrawl begin");
		String site = "jd";
		List<CrawlB2CRetryTask> tasks = crawlB2CRetryTaskDao.listRetryTasks(site);
		if (tasks.isEmpty()) {
			LOGGER.info("没有查询到需要重试的失败任务");
			return;
		}
		for (CrawlB2CRetryTask task : tasks) {
			if (task.getRetryNum() >= maxRetryNum) {
				LOGGER.info("已经超过最大重试次数,不能再继续执行,taskId is {}", task.getId());
				task.setStatus(CrawlTaskStatusEnum.PASSED.getCode());
				task.setUpdateTime(new Date());
				crawlB2CRetryTaskDao.update(task);
				memberIntenetInfoDao.updateJDAuthorFlagByUserId(task.getUserIdentity(),
						AuthorStatusEnum.FAIL.getStatus());
				continue;
			}
			service.crawlingJD(task.getUserIdentity(), task.getUserChannel(), task.getUserCity(), task.getId());
		}

		LOGGER.info("retryCrawl end");
	}

	/**
	 * 处理京东未授权用户
	 */
	public void unAuthorCrawl() {

		LOGGER.info("定时任务对未授权的京东用户进行授权 start");

		List<MemberIntenetInfo> memberIntenetInfoList = memberIntenetInfoDao
				.queryMemberIntenetInfoByJDAuthorFlag(AuthorStatusEnum.UNAUTHOR.getStatus());

		if (memberIntenetInfoList.isEmpty()) {
			LOGGER.info("未查询到未授权的京东用户");
			return;
		}

		for (MemberIntenetInfo obj : memberIntenetInfoList) {

			service.crawlingJD(obj.getsUserId(), "my", null, null);

		}
		LOGGER.info("定时任务对未授权的京东用户进行授权 end");

	}

}
