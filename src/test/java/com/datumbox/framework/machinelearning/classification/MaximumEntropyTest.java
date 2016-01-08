/**
 * Copyright (C) 2013-2016 Vasilis Vryniotis <bbriniotis@datumbox.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datumbox.framework.machinelearning.classification;

import com.datumbox.common.dataobjects.Dataframe;
import com.datumbox.common.dataobjects.Record;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConfiguration;
import com.datumbox.tests.TestConfiguration;
import com.datumbox.tests.abstracts.AbstractTest;
import com.datumbox.tests.Datasets;
import com.datumbox.tests.utilities.TestUtils;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for MaximumEntropy.
 *
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class MaximumEntropyTest extends AbstractTest {

    /**
     * Test of validate method, of class MaximumEntropy.
     */
    @Test
    public void testValidate() {
        logger.info("validate");
        
        DatabaseConfiguration dbConf = TestUtils.getDBConfig();
        
        
        Dataframe[] data = Datasets.carsNumeric(dbConf);
        
        Dataframe trainingData = data[0];
        Dataframe validationData = data[1];
        
        
        String dbName = this.getClass().getSimpleName();
        MaximumEntropy instance = new MaximumEntropy(dbName, dbConf);
        
        MaximumEntropy.TrainingParameters param = new MaximumEntropy.TrainingParameters();
        param.setTotalIterations(10);
        
        instance.fit(trainingData, param);
        
        instance.close();
        //instance = null;
        instance = new MaximumEntropy(dbName, dbConf);
        
        instance.validate(validationData);
        
        Map<Integer, Object> expResult = new HashMap<>();
        Map<Integer, Object> result = new HashMap<>();
        for(Map.Entry<Integer, Record> e : validationData.entries()) {
            Integer rId = e.getKey();
            Record r = e.getValue();
            expResult.put(rId, r.getY());
            result.put(rId, r.getYPredicted());
        }
        assertEquals(expResult, result);
        
        instance.delete();
        
        trainingData.delete();
        validationData.delete();
    }


    /**
     * Test of kFoldCrossValidation method, of class MaximumEntropy.
     */
    @Test
    public void testKFoldCrossValidation() {
        logger.info("kFoldCrossValidation");
        
        DatabaseConfiguration dbConf = TestUtils.getDBConfig();
        
        int k = 5;
        
        Dataframe[] data = Datasets.carsNumeric(dbConf);
        Dataframe trainingData = data[0];
        data[1].delete();
        
        
        String dbName = this.getClass().getSimpleName();
        MaximumEntropy instance = new MaximumEntropy(dbName, dbConf);
        
        MaximumEntropy.TrainingParameters param = new MaximumEntropy.TrainingParameters();
        param.setTotalIterations(10);
        
        MaximumEntropy.ValidationMetrics vm = instance.kFoldCrossValidation(trainingData, param, k);
        
        double expResult = 0.6051098901098901;
        double result = vm.getMacroF1();
        assertEquals(expResult, result, TestConfiguration.DOUBLE_ACCURACY_HIGH);
        instance.delete();
        
        trainingData.delete();
    }

    
}
