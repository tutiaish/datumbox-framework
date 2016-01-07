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
package com.datumbox.framework.machinelearning.regression;

import com.datumbox.framework.machinelearning.common.bases.basemodels.BaseLinearRegression;
import com.datumbox.common.dataobjects.AssociativeArray;
import com.datumbox.common.dataobjects.Dataframe;
import com.datumbox.common.dataobjects.Record;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConfiguration;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConnector;
import com.datumbox.common.dataobjects.TypeInference;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConnector.MapType;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConnector.StorageHint;


import java.util.Map;

/**
 * Linear Regression model which uses the Normalised Least Mean Squares Algorithm.
 * This implementation should be preferred from MatrixLinearRegression when the 
 data can't fit the memory.
 
 References:
 http://cs229.stanford.edu/notes/cs229-notes1.pdf
 http://www.holehouse.org/mlclass/04_Linear_Regression_with_multiple_variables.html
 https://class.coursera.org/ml-003/lecture/index
 * 
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class NLMS extends BaseLinearRegression<NLMS.ModelParameters, NLMS.TrainingParameters, NLMS.ValidationMetrics> {
     
    /** {@inheritDoc} */
    public static class ModelParameters extends BaseLinearRegression.ModelParameters {
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
    public static class TrainingParameters extends BaseLinearRegression.TrainingParameters {  
        private static final long serialVersionUID = 1L;
        
        private int totalIterations=1000; 
        private double learningRate=0.1;

        /**
         * Getter for the total iterations of the training process.
         * 
         * @return 
         */
        public int getTotalIterations() {
            return totalIterations;
        }

        /**
         * Setter for the total iterations of the training process.
         * 
         * @param totalIterations 
         */
        public void setTotalIterations(int totalIterations) {
            this.totalIterations = totalIterations;
        }

        /**
         * Getter for the initial value of the Learning Rate.
         * 
         * @return 
         */
        public double getLearningRate() {
            return learningRate;
        }

        /**
         * Setter for the initial value of the Learning Rate. This value will be
         * adapted during the iterations.
         * 
         * @param learningRate 
         */
        public void setLearningRate(double learningRate) {
            this.learningRate = learningRate;
        }

    } 
    
    /** {@inheritDoc} */
    public static class ValidationMetrics extends BaseLinearRegression.ValidationMetrics {
        private static final long serialVersionUID = 1L;
        
    }

    /**
     * Public constructor of the algorithm.
     * 
     * @param dbName
     * @param dbConf 
     */
    public NLMS(String dbName, DatabaseConfiguration dbConf) {
        super(dbName, dbConf, NLMS.ModelParameters.class, NLMS.TrainingParameters.class, NLMS.ValidationMetrics.class);
    }

    @Override
    protected void _fit(Dataframe trainingData) {
        ModelParameters modelParameters = knowledgeBase.getModelParameters();
        
        Map<Object, Double> thitas = modelParameters.getThitas();
        
        //we initialize the thitas to zero for all features
        thitas.put(Dataframe.COLUMN_NAME_CONSTANT, 0.0);
        for(Object feature : trainingData.getXDataTypes().keySet()) {
            thitas.put(feature, 0.0);
        }
        
        TrainingParameters trainingParameters = knowledgeBase.getTrainingParameters();

        double minError = Double.POSITIVE_INFINITY;
        
        double learningRate = trainingParameters.getLearningRate();
        int totalIterations = trainingParameters.getTotalIterations();
        DatabaseConnector dbc = knowledgeBase.getDbc();
        for(int iteration=0;iteration<totalIterations;++iteration) {
            
            logger.debug("Iteration {}", iteration);
            
            Map<Object, Double> tmp_newThitas = dbc.getBigMap("tmp_newThitas", MapType.HASHMAP, StorageHint.IN_MEMORY, true);
            
            tmp_newThitas.putAll(thitas);
            
            batchGradientDescent(trainingData, tmp_newThitas, learningRate);
            //stochasticGradientDescent(trainingData, newThitas, learningRate);
            
            double newError = calculateError(trainingData,tmp_newThitas);
            
            //bold driver
            if(newError>minError) {
                learningRate/=2.0;
            }
            else {
                learningRate*=1.05;
                minError=newError;
                
                //keep the new thitas
                thitas.clear();
                thitas.putAll(tmp_newThitas);
            }
            
            //Drop the temporary Collection
            dbc.dropBigMap("tmp_newThitas", tmp_newThitas);
        }
    }

    @Override
    protected void predictDataset(Dataframe newData) {
        Map<Object, Double> thitas = knowledgeBase.getModelParameters().getThitas();
        
        for(Map.Entry<Integer, Record> e : newData.entries()) {
            Integer rId = e.getKey();
            Record r = e.getValue();
            double yPredicted = hypothesisFunction(r.getX(), thitas);
            newData._unsafe_set(rId, new Record(r.getX(), r.getY(), yPredicted, r.getYPredictedProbabilities()));
        }
    }
    
    private void batchGradientDescent(Dataframe trainingData, Map<Object, Double> newThitas, double learningRate) {
        //NOTE! This is not the stochastic gradient descent. It is the batch gradient descent optimized for speed (despite it looks more than the stochastic). 
        //Despite the fact that the loops are inverse, the function still changes the values of Thitas at the end of the function. We use the previous thitas 
        //to estimate the costs and only at the end we update the new thitas.
        ModelParameters modelParameters = knowledgeBase.getModelParameters();
        
        double multiplier = learningRate/modelParameters.getN();
        Map<Object, Double> thitas = modelParameters.getThitas();
        
        for(Record r : trainingData) { 
            //mind the fact that we use the previous thitas to estimate the new ones! this is because the thitas must be updated simultaniously
            double error = TypeInference.toDouble(r.getY()) - hypothesisFunction(r.getX(), thitas);
            
            double errorMultiplier = multiplier*error;
            
            
            //update the weight of constant
            newThitas.put(Dataframe.COLUMN_NAME_CONSTANT, newThitas.get(Dataframe.COLUMN_NAME_CONSTANT)+errorMultiplier);

            //update the rest of the weights
            for(Map.Entry<Object, Object> entry : r.getX().entrySet()) {
                Object feature = entry.getKey();
                
                Double thitaWeight = newThitas.get(feature);
                if(thitaWeight!=null) {//ensure that the feature is in the supported features
                    Double value = TypeInference.toDouble(entry.getValue());
                    newThitas.put(feature, thitaWeight+errorMultiplier*value);
                }
            }
        }
    }
    
    /*
    private void stochasticGradientDescent(Dataframe trainingData, Map<Object, Double> newThitas, double learningRate) {
        double multiplier = learningRate/knowledgeBase.getModelParameters().getN();
        
        for(Record r : trainingData) { 
            //mind the fact that we use the new thitas to estimate the cost! 
            double error = TypeInference.toDouble(r.getY()) - hypothesisFunction(r.getX(), newThitas);
            
            double errorMultiplier = multiplier*error;
            
            
            //update the weight of constant
            newThitas.put(Dataframe.COLUMN_NAME_CONSTANT, newThitas.get(Dataframe.COLUMN_NAME_CONSTANT)+errorMultiplier);

            //update the rest of the weights
            for(Map.Entry<Object, Object> entry : r.getX().entrySet()) {
                Object feature = entry.getKey();
                
                Double thitaWeight = newThitas.get(feature);
                if(thitaWeight!=null) {//ensure that the feature is in the supported features
                    Double value = TypeInference.toDouble(entry.getValue());
                    newThitas.put(feature, thitaWeight+errorMultiplier*value);
                }
            }
        }
    }
    */
    
    private double calculateError(Dataframe trainingData, Map<Object, Double> thitas) {
        //The cost function as described on http://ufldl.stanford.edu/wiki/index.php/Softmax_Regression
        //It is optimized for speed to reduce the amount of loops
        double error=0.0;
        
        for(Map.Entry<Integer, Record> e : trainingData.entries()) {
            Integer rId = e.getKey();
            Record r = e.getValue();
            double yPredicted = hypothesisFunction(r.getX(), thitas);
            trainingData._unsafe_set(rId, new Record(r.getX(), r.getY(), yPredicted, r.getYPredictedProbabilities()));
            error+=Math.pow(TypeInference.toDouble(r.getY()) -yPredicted, 2);
        }
        
        return error;
    }
    
    private double hypothesisFunction(AssociativeArray x, Map<Object, Double> thitas) {
        double sum = thitas.get(Dataframe.COLUMN_NAME_CONSTANT);
        
        for(Map.Entry<Object, Object> entry : x.entrySet()) {
            Object feature = entry.getKey();
            
            Double thitaWeight = thitas.get(feature);
            if(thitaWeight!=null) {//ensure that the feature is in the supported features
                Double xj = TypeInference.toDouble(entry.getValue());
                sum+=thitaWeight*xj;
            }
        }
        
        return sum;
    }
}
