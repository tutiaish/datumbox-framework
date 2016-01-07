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
package com.datumbox.framework.machinelearning.featureselection.continuous;

import com.datumbox.common.dataobjects.Dataframe;
import com.datumbox.common.dataobjects.Record;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConfiguration;
import com.datumbox.common.dataobjects.TypeInference;
import com.datumbox.configuration.TestConfiguration;
import com.datumbox.tests.bases.BaseTest;
import com.datumbox.tests.utilities.Datasets;
import com.datumbox.tests.utilities.TestUtils;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class PCATest extends BaseTest {

    /**
     * Test of selectFeatures method, of class PCA.
     */
    @Test
    public void testSelectFeatures() {
        logger.info("selectFeatures");
        
        DatabaseConfiguration dbConf = TestUtils.getDBConfig();
        
        Dataframe[] data = Datasets.featureTransformationPCA(dbConf);
        
        Dataframe originalData = data[0];
        Dataframe validationdata = data[0].copy();
        Dataframe expResult = data[1];
        
        String dbName = this.getClass().getSimpleName();
        PCA instance = new PCA(dbName, dbConf);
        
        PCA.TrainingParameters param = new PCA.TrainingParameters();
        param.setMaxDimensions(null);
        
        instance.fit_transform(originalData, param);
        instance.close();
        instance=null;
        
        instance = new PCA(dbName, dbConf);
        
        instance.transform(validationdata);
        
        assertEquals(validationdata.size(), expResult.size());
        
        Iterator<Record> itResult = validationdata.iterator();
        Iterator<Record> itExpectedResult = expResult.iterator();
        
        
        while(itResult.hasNext()) {
            Record r1 = itResult.next();
            Record r2 = itExpectedResult.next();
            
            for(Map.Entry<Object, Object> entry : r1.getX().entrySet()) {
                Object feature = entry.getKey();
                Double value = TypeInference.toDouble(entry.getValue());
                
                assertEquals(TypeInference.toDouble(r2.getX().get(feature)), value, TestConfiguration.DOUBLE_ACCURACY_MEDIUM);
            }
        }
        
        instance.delete();
        
        originalData.delete();
        validationdata.delete();
        expResult.delete();
    }
    
}
