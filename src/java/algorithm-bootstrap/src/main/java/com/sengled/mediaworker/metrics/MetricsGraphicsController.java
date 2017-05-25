package com.sengled.mediaworker.metrics;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.net.HttpHeaders;
import com.sengled.mediaworker.algorithm.StreamingContextManager;
import com.sengled.mediaworker.metrics.osmonitor.OSMonitor;

/**
 * 用来生成统计图
 * 
 * @author chenxh
 */
@Controller
public class MetricsGraphicsController implements InitializingBean, MetricsGraphics {
	private static final Logger LOGGER = LoggerFactory.getLogger(MetricsGraphicsController.class);

	@Autowired
	private MetricRegistry metricRegistry;
	@Autowired
	private StreamingContextManager streamingContextManager;
	
	private final ConcurrentHashMap<String, List<Graphics>> graphicsList = new ConcurrentHashMap<String, List<Graphics>>();
	
	@Override
	public void afterPropertiesSet() throws Exception {

		
		
		final OSMonitor monitor = OSMonitor.getInstance();
		
		// id, CPU 空闲
		final String osMetrics = "os";
		metricRegistry.register(MetricRegistry.name(osMetrics, "cpuIdle"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                int idle = monitor.getSystemCpuIdle();

                return idle;
            }
        });

        // us, 用户 CPU 使用率
		metricRegistry.register(MetricRegistry.name(osMetrics, "cpuLoad"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                int processCpuLoad = monitor.getProcessCpuLoad();
                return processCpuLoad;
            }
        });
        
        // heap memory
		metricRegistry.register(MetricRegistry.name(osMetrics, "heapMemory"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                long heapMemoryUsed = monitor.getHeapMemoryUsed();
                return heapMemoryUsed;
            }
        });
        
        // non head memory
		metricRegistry.register(MetricRegistry.name(osMetrics, "nonHeapMemory"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                final Long nonHeapMemoryUsed = monitor.getNonHeapMemoryUsed();
				return nonHeapMemoryUsed;
            }
        });
		
		
		GraphicsReporter reporter =  GraphicsReporter.forRegistry(metricRegistry)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.SECONDS)
				.build(this);
		reporter.start(25, TimeUnit.SECONDS);
	}

	@Override
	public Graphics getOrCreateGraphics(String name, String type, String colTemplates) {
		List<Graphics> tables = graphicsList.get(type);
		if (null == tables) {
			tables = new ArrayList<Graphics>();
			graphicsList.put(type, tables);
		}

		for (Graphics table : tables) {
			if (StringUtils.equals(name, table.getName())) {
				return table;
			}
		}

		Graphics table = null;
		if (null != colTemplates) {
			table = new Graphics(name, colTemplates);
			tables.add(table);
			
			LOGGER.info("add {} griphics, name = {}, columns = {}", type, name, colTemplates);
			
		}
		return table;
	}

	//@GetMapping(path="/graphics/{type}")
	@RequestMapping(value="/graphics/{type}",method=RequestMethod.GET)
	public ResponseEntity<?> getGraphics(
			@PathVariable(value="type") String type, 
			@RequestParam(name="name", required=true) String name,
			@RequestParam(name="column", required=false, defaultValue="value") String column) throws IOException {
		List<Graphics> graphics = graphicsList.get(type.toUpperCase());
		if (null == graphics) {
			return ResponseEntity.notFound().build();
		}

		for (Graphics item : graphics) {
			if (item.getName().equals(name)) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				boolean foundColumn = item.ouput(out, column);
				
				if (foundColumn) {
					return ResponseEntity.ok().header(HttpHeaders.CONNECTION, "Close").body(out.toString("UTF-8"));
				}
			}
		}

		return ResponseEntity.notFound().build();
	}
	
	
	@RequestMapping(value="/tokens/list",method=RequestMethod.GET)
	@ResponseBody
	public List<String>  tokensList() throws IOException {
		List<String> tokens = streamingContextManager.getToken();
		return tokens;
	}
}
