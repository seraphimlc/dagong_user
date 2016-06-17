package com.dagong.service;

import com.alibaba.fastjson.JSON;
import com.dagong.pojo.Job;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by liuchang on 16/4/5.
 */
@Service
public class SearchService {

    private TransportClient transportClient = null;
    @Value("${searchEngine.address}")
    private String searchAddress = null;

    @Value("${searchEngine.port}")
    private int searchPort = 0;
    private static String JOB_INDEX = "job";
    private static String JOB_TYPE = "info";

    @PostConstruct
    public void init() {
        try {
            transportClient = TransportClient.builder().build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(searchAddress), searchPort));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public List<Map> searchJobByType(int jobType) {
        List<Map> jobs = new ArrayList<>();
        SearchResponse searchResponse = transportClient.prepareSearch(JOB_INDEX).setTypes(JOB_TYPE)
                .setQuery(QueryBuilders.termQuery("jobType", jobType)).execute().actionGet();
        System.out.println("searchResponse = " + searchResponse);
        for (SearchHit searchHitFields : searchResponse.getHits().getHits()) {
            jobs.add(searchHitFields.getSource());
        }
        return jobs;
    }


    public Map getJob(String jobId) {
        GetResponse response = transportClient.prepareGet(JOB_INDEX, JOB_TYPE, jobId).execute().actionGet();
        if (response.isExists()) {
            return response.getSource();
        }
        return null;
    }

    public boolean addUserFollowJobToIndex() {
        return true;
    }

    public boolean addJobToIndex(List<Job> jobList) {
        if (jobList == null || jobList.isEmpty()) {
            return false;
        }

        BulkRequestBuilder bulkRequestBuilder = transportClient.prepareBulk();
        jobList.forEach(job -> {
            bulkRequestBuilder.add(transportClient.prepareIndex(JOB_INDEX, JOB_TYPE, job.getId()).setSource(JSON.toJSONString(job)));
        });
        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        return true;
    }

    public String getSearchAddress() {
        return searchAddress;
    }

    public void setSearchAddress(String searchAddress) {
        this.searchAddress = searchAddress;
    }

    public int getSearchPort() {
        return searchPort;
    }

    public void setSearchPort(int searchPort) {
        this.searchPort = searchPort;
    }
}
