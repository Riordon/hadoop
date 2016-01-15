package com.luffy.hadoop;

import java.io.IOException;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleMapReduce extends Configured implements Tool{
	private static Logger logger = LoggerFactory.getLogger(ModuleMapReduce.class);
	
	public static void main(String[] args) {
		Configuration conf = new Configuration();
		conf.set("mapreduce.map.output.compress","true");
		conf.set("mapreduce.map.output.compress.codec","org.apache.hadoop.io.compress.Lz4Codec");
		
		int status = 0;
		try {
			status = ToolRunner.run(conf, new ModuleMapReduce(), args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(status);
	}

	public int run(String[] args) throws Exception {
		Configuration conf = super.getConf();
		Job job = Job.getInstance(conf);
		job.setJarByClass(ModuleMapReduce.class);
		job.setMapperClass(ModuleMapper.class);
		job.setReducerClass(ModuleReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		Path input = new Path(args[0]);
		Path outpath = new Path(args[1]);
		
		FileInputFormat.setInputPaths(job, input);
		FileOutputFormat.setOutputPath(job, outpath);
		
		if (job.waitForCompletion(true)) {
			System.out.println("Job completed successfully.");
			return 0;
		}
		
		return 1;
	}

	public static class ModuleMapper extends Mapper<LongWritable, Text, Text, Text> {

		private Text documentId;
		private Text word = new Text();
		
		@Override
		protected void map(LongWritable key, Text value,
				Mapper<LongWritable, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			String filename = ((FileSplit)context.getInputSplit()).getPath().getName();
			logger.info("-------------------------");
			logger.info(filename);
			logger.info("-------------------------");
			documentId = new Text(filename);
			
			for (String token : StringUtils.split(value.toString(), ' ')) {
				word.set(token);
				context.write(word, documentId);
			}
		}

		@Override
		protected void setup(
				Mapper<LongWritable, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
		}
		
	}
	
	public static class ModuleReducer extends Reducer<Text, Text, Text, Text> {
		private Text docIds = new Text();
		
		@Override
		protected void reduce(Text key, Iterable<Text> values,
				Reducer<Text, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			HashSet<Text> uniqueDocIds = new HashSet<Text>();
			for (Text docId : values) {
				uniqueDocIds.add(docId);
			}
			
			docIds.set(new Text(StringUtils.join(",", uniqueDocIds)));
			context.write(key, docIds);
		}
		
	}
}
