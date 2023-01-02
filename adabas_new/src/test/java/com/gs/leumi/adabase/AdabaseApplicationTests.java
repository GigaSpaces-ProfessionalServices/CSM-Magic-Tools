package com.gs.leumi.adabase;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.leumi.common.model.LfpmTnuotHayom;
import com.gs.leumi.common.model.LfpmTnuotHayomPmolTnuot;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

@SpringBootTest
@ContextConfiguration()
class AdabaseApplicationTests {
	@Autowired
	private com.gs.leumi.adabase.config.Configuration config;
	@Autowired
	private FileClient fileClient;

	private static final Logger logger = LoggerFactory.getLogger(AdabaseApplicationTests.class);



	@Test
	void contextLoads() {
		try {
//			generateTestFile();
			fileClient.run();
		} catch (Exception e ){
			e.printStackTrace();
		}
	}


	private void generateTestFile(){

		{
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(fileClient.getPath()));
				ObjectMapper mapper = new ObjectMapper();
				mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
				for (int i = 0; i < 10; i++) {
					LfpmTnuotHayom lfpmTnuotHayom = LfpmTnuotHayom.generateRandom();
					if (lfpmTnuotHayom != null) {
						try {
							String msg = mapper.writeValueAsString(lfpmTnuotHayom);
							writer.write(msg.substring(0,msg.lastIndexOf(',')) + "}");
							writer.newLine();
						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
						int sons = new Random(System.currentTimeMillis()).nextInt(10);
						for(int j = 0; j<sons; j++){
							LfpmTnuotHayomPmolTnuot tnuot = LfpmTnuotHayomPmolTnuot.generateRandom(lfpmTnuotHayom.getIsn());
							try {
								String msg = mapper.writeValueAsString(tnuot);
								writer.write(msg);
								writer.newLine();
							} catch (JsonProcessingException e) {
								e.printStackTrace();
							}

						}

					}
				}
				writer.flush();
				writer.close();
			} catch (IOException ioe){
				ioe.printStackTrace();
			}
		}

	}

}
