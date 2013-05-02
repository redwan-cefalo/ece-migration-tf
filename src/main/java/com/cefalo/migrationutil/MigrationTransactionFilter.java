package com.cefalo.migrationutil;

import com.escenic.domain.ContentDescriptor;
import com.escenic.domain.ContentSummary;
import com.escenic.domain.PropertyDescriptor;
import neo.xredsys.api.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * User: redwan
 * Date: 4/29/13
 * Time: 4:33 PM
 */
public class MigrationTransactionFilter implements TransactionFilter {
    Logger logger = Logger.getLogger(MigrationTransactionFilter.class);
    ObjectLoader loader;
    String[] relationTypes = {"TEASERREL","TOPMEDIAREL", "STORYREL"};
    public MigrationTransactionFilter() {
        this.loader = IOAPI.getAPI().getObjectLoader();
    }

    public void doCreate(IOTransaction ioTransaction) throws FilterException {
        ArticleTransaction transaction = null;
        if (ioTransaction instanceof ArticleTransaction) {
            transaction = (ArticleTransaction) ioTransaction;
        } else {
            return;
        }
        if(!"escenic-migration".equals(transaction.getSource())) {
            logger.debug("not a migration article. ignoring...");
            return;
        }
        for (String relationType : relationTypes)  {
            List<ContentSummary> summaryList = transaction.getContentSummaries(relationType);
            List<ContentSummary> newSummaryList = new ArrayList<ContentSummary>();

            for (ContentSummary summary : summaryList) {
                int relatedId = IOHashKey.valueOf(summary.getContentLink().getHref()).getObjectId();
                logger.debug("loading article id : " + relatedId);
                Article relatedArticle = loader.getArticle(relatedId);

                List<PropertyDescriptor> propertyDescriptors = summary.getDescriptor().getPropertyDescriptors();

                for(PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (summary.getProperty(propertyDescriptor) != null && !"".equals(summary.getProperty(propertyDescriptor).toString().trim())) {
                        logger.debug("Summary field already has a value. Skiping...");
                        continue;
                    }
                    String fieldName = propertyDescriptor.getName();
                    FieldValue field = null;
                    try{
                        field = relatedArticle.getField(fieldName);
                    } catch (IllegalArgumentException ex) {
                        logger.debug("field '" + fieldName +"' does not exist");
                    }
                    if (field != null) {
                        Object fieldValue = field.getValue();
                        logger.debug("setting summary field " + fieldName + " value = " + fieldValue);
                        summary.setProperty(propertyDescriptor, fieldValue);
                    }
                }
                newSummaryList.add(summary);
            }
            logger.debug("setting new content summaries");
            transaction.setContentSummaries(relationType, newSummaryList);
        }
//        for (String relationType : relationTypes)  {
//            List<ContentSummary> summaryList = transaction.getContentSummaries(relationType);
//            List<ContentSummary> newSummaryList = new ArrayList<ContentSummary>();
//            logger.debug("removed all from content summaries for relation " + relationType);
//            for (int i = 0; i < summaryList.size(); i++) {
//                int relatedId = IOHashKey.valueOf(summaryList.get(i).getContentLink().getHref()).getObjectId();
//                logger.debug("loading article id : " + relatedId);
//                Article relatedArticle = loader.getArticle(relatedId);
//                ContentSummary newContentSummary = relatedArticle.toContentSummary();
//                logger.debug("newContentSummary = " + newContentSummary);
//                newSummaryList.add( newContentSummary);
//            }
//            logger.debug("setting new content summaries for relation " + relationType);
//            transaction.setContentSummaries(relationType, newSummaryList);
//            transaction.setContentSummaries(relationType.toLowerCase(), newSummaryList);
//
//        }
    }

    public void doDelete(IOTransaction ioTransaction) throws FilterException {
        // do nothing;
    }

    public void doUpdate(IOTransaction ioTransaction) throws FilterException {
        doCreate(ioTransaction);
    }

    public boolean getErrorsAreFatal() {
        return false;
    }

    public boolean isEnabled() {
        return true;
    }
}
