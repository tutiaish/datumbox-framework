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

import com.datumbox.common.persistentstorage.interfaces.DatabaseConfiguration;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConnector;
import com.datumbox.framework.machinelearning.common.abstracts.algorithms.AbstractNaiveBayes;


/**
 * The MultinomialNaiveBayes class provides an implementation of Multinomial
 * Naive Bayes model.
 * 
 * References:
 * http://blog.datumbox.com/machine-learning-tutorial-the-naive-bayes-text-classifier/
 * 
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class MultinomialNaiveBayes extends AbstractNaiveBayes<MultinomialNaiveBayes.ModelParameters, MultinomialNaiveBayes.TrainingParameters, MultinomialNaiveBayes.ValidationMetrics> {
    
    /** {@inheritDoc} */
    public static class ModelParameters extends AbstractNaiveBayes.ModelParameters {
        private static final long serialVersionUID = 1L;
        
        /** 
         * @param dbc
         * @see com.datumbox.framework.machinelearning.common.bases.baseobjects.BaseModelParameters#BaseModelParameters(com.datumbox.common.persistentstorage.interfaces.DatabaseConnector) 
         */
        protected ModelParameters(DatabaseConnector dbc) {
            super(dbc);
        }
        
    } 
    
    /** {@inheritDoc} */
    public static class TrainingParameters extends AbstractNaiveBayes.TrainingParameters {    
        private static final long serialVersionUID = 1L;
        
    } 
    
    /** {@inheritDoc} */
    public static class ValidationMetrics extends AbstractNaiveBayes.ValidationMetrics {
        private static final long serialVersionUID = 1L;

    }

    /**
     * Public constructor of the algorithm.
     * 
     * @param dbName
     * @param dbConf 
     */
    public MultinomialNaiveBayes(String dbName, DatabaseConfiguration dbConf) {
        super(dbName, dbConf, MultinomialNaiveBayes.ModelParameters.class, MultinomialNaiveBayes.TrainingParameters.class, MultinomialNaiveBayes.ValidationMetrics.class);
    }
    
}