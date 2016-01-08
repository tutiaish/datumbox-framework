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
package com.datumbox.applications.datamodeling;

import com.datumbox.common.dataobjects.Dataframe;
import com.datumbox.common.dataobjects.Record;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConfiguration;
import com.datumbox.tests.TestConfiguration;
import com.datumbox.framework.machinelearning.classification.MultinomialNaiveBayes;
import com.datumbox.framework.machinelearning.datatransformation.DummyXMinMaxNormalizer;
import com.datumbox.tests.abstracts.AbstractTest;
import com.datumbox.tests.Datasets;
import com.datumbox.tests.utilities.TestUtils;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for Modeler.
 *
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class ModelerTest extends AbstractTest {

    /**
     * Test of train and predict method, of class Modeler.
     */
    @Test
    public void testTrainAndValidate() {
        logger.info("testTrainAndValidate");
        
        DatabaseConfiguration dbConf = TestUtils.getDBConfig();
        
        Dataframe[] data = Datasets.carsNumeric(dbConf);
        Dataframe trainingData = data[0];
        
        Dataframe validationData = data[1];
        
        
        String dbName = this.getClass().getSimpleName();
        
        Modeler instance = new Modeler(dbName, dbConf);
        Modeler.TrainingParameters trainingParameters = new Modeler.TrainingParameters();
        
        
        //Model Configuration
        
        trainingParameters.setMLmodelClass(MultinomialNaiveBayes.class);
        MultinomialNaiveBayes.TrainingParameters modelTrainingParameters = new MultinomialNaiveBayes.TrainingParameters();
        modelTrainingParameters.setMultiProbabilityWeighted(true);
        trainingParameters.setMLmodelTrainingParameters(modelTrainingParameters);

        //data transfomation configuration
        trainingParameters.setDataTransformerClass(DummyXMinMaxNormalizer.class);
        DummyXMinMaxNormalizer.TrainingParameters dtParams = new DummyXMinMaxNormalizer.TrainingParameters();
        trainingParameters.setDataTransformerTrainingParameters(dtParams);
        
        //feature selection configuration
        trainingParameters.setFeatureSelectionClass(null);
        trainingParameters.setFeatureSelectionTrainingParameters(null);
        
        instance.fit(trainingData, trainingParameters);
        
        
        MultinomialNaiveBayes.ValidationMetrics vm = (MultinomialNaiveBayes.ValidationMetrics) instance.validate(trainingData);
        
        instance.setValidationMetrics(vm);
        
        double expResult2 = 0.8;
        assertEquals(expResult2, vm.getMacroF1(), TestConfiguration.DOUBLE_ACCURACY_HIGH);
        instance.close();
        //instance = null;
        
        
        logger.info("validate");
        
        
        instance = new Modeler(dbName, dbConf);
        
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
    
}
