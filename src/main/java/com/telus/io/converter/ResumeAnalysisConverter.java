package com.telus.io.converter;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.telus.io.model.ResumeAnalysis;


/**
 * Custom converter that explicitly implements Converter<String, ResumeAnalysis>
 * to help Spring determine the source and target types.
 */
@Component
public class ResumeAnalysisConverter extends BeanOutputConverter<ResumeAnalysis> 
        implements Converter<String, ResumeAnalysis> {
    
    public ResumeAnalysisConverter() {
        super(ResumeAnalysis.class);
    }
    
    @Override
    public ResumeAnalysis convert(String source) {
        return super.convert(source);
    }
}
