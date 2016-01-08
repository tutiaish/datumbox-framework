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
package com.datumbox.framework.machinelearning.topicmodeling;

import com.datumbox.common.dataobjects.Dataframe;
import com.datumbox.common.dataobjects.Record;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConfiguration;
import com.datumbox.tests.TestConfiguration;
import com.datumbox.framework.machinelearning.classification.SoftMaxRegression;
import com.datumbox.framework.machinelearning.common.abstracts.modelers.AbstractClassifier;
import com.datumbox.framework.utilities.text.extractors.UniqueWordSequenceExtractor;
import com.datumbox.tests.abstracts.AbstractTest;
import com.datumbox.tests.utilities.TestUtils;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for LatentDirichletAllocation.
 *
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class LatentDirichletAllocationTest extends AbstractTest {
    
    /**
     * Test of validate method, of class LatentDirichletAllocation.
     */
    @Test
    public void testValidate() {
        logger.info("validate");
        
        DatabaseConfiguration dbConf = TestUtils.getDBConfig();
        
        
        String dbName = this.getClass().getSimpleName();

        
        Map<Object, URI> dataset = new HashMap<>();
        try {
            dataset.put("negative", TestUtils.getRemoteFile(new URL("http://www.datumbox.com/files/datasets/example.neg")));
            dataset.put("positive", TestUtils.getRemoteFile(new URL("http://www.datumbox.com/files/datasets/example.pos")));
        }
        catch(UncheckedIOException | MalformedURLException ex) {
            logger.warn("Unable to download datasets, skipping test.");
            throw new RuntimeException(ex);
        }
        
        UniqueWordSequenceExtractor wsExtractor = new UniqueWordSequenceExtractor(new UniqueWordSequenceExtractor.Parameters());
        
        Dataframe trainingData = Dataframe.Builder.parseTextFiles(dataset, wsExtractor, dbConf);
        
        
        LatentDirichletAllocation lda = new LatentDirichletAllocation(dbName, dbConf);
        
        LatentDirichletAllocation.TrainingParameters trainingParameters = new LatentDirichletAllocation.TrainingParameters();
        trainingParameters.setMaxIterations(15);
        trainingParameters.setAlpha(0.01);
        trainingParameters.setBeta(0.01);
        trainingParameters.setK(25);        
        
        lda.fit(trainingData, trainingParameters);
        
        lda.validate(trainingData);
        
        Dataframe reducedTrainingData = new Dataframe(dbConf);
        for(Record r : trainingData) {
            //take the topic assignments and convert them into a new Record
            reducedTrainingData.add(new Record(r.getYPredictedProbabilities(), r.getY()));
        }
        
        SoftMaxRegression smr = new SoftMaxRegression(dbName, dbConf);
        SoftMaxRegression.TrainingParameters tp = new SoftMaxRegression.TrainingParameters();
        tp.setLearningRate(1.0);
        tp.setTotalIterations(50);
        
        AbstractClassifier.ValidationMetrics vm = smr.kFoldCrossValidation(reducedTrainingData, tp, 1);
        
        double expResult = 0.6859007513066202;
        double result = vm.getMacroF1();
        assertEquals(expResult, result, TestConfiguration.DOUBLE_ACCURACY_HIGH);

        smr.delete();
        lda.delete();
        reducedTrainingData.delete();
        
        trainingData.delete();
    }

    
}
