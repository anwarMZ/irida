package ca.corefacility.bioinformatics.irida.ria.dialect.onsen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.Arguments;
import org.thymeleaf.Configuration;
import org.thymeleaf.dom.Attribute;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.Node;
import org.thymeleaf.processor.AbstractProcessor;
import org.thymeleaf.processor.IProcessorMatcher;
import org.thymeleaf.processor.ProcessorMatchingContext;
import org.thymeleaf.processor.ProcessorResult;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

import java.util.Map;

/**
 * @author Josh Adam <josh.adam@phac-aspc.gc.ca>
 */
public class OnsenProcessor extends AbstractProcessor {

	@Override
	public int getPrecedence() {
		return 1200;
	}

	@Override
	protected ProcessorResult doProcess(Arguments arguments, ProcessorMatchingContext processorMatchingContext, Node node) {
		Map<String, Attribute> attributeMap;
		Element element = ((Element) node);
		attributeMap = element.getAttributeMap();
		String dialectPrefix = processorMatchingContext.getDialectPrefix();

		for (Attribute attribute : attributeMap.values()) {

			String dataAttrName = Attribute
					.getUnprefixedAttributeName(attribute.getNormalizedName());

			String attributeName = attribute.getNormalizedName();
			String normalizedPrefix = Attribute
					.getPrefixFromAttributeName(attribute.getNormalizedName());
			if (dialectPrefix.equals(normalizedPrefix)) {
				final String attributeValue = element
						.getAttributeValue(attributeName);

				Configuration configuration = arguments.getConfiguration();
				final IStandardExpressionParser expressionParser = StandardExpressions
						.getExpressionParser(configuration);
				IStandardExpression expression = expressionParser
						.parseExpression(arguments.getConfiguration(),
								arguments, attributeValue);

				final Object result = expression.execute(configuration,
						arguments);

				if (result != null) {
					element.setAttribute(
							dataAttrName,
							result.toString());
				}

				element.removeAttribute(attributeName);

			}

		}

		return ProcessorResult.ok();
	}

	@Override
	public IProcessorMatcher<? extends Node> getMatcher() {
		return new OnsenAttributeMatcher();
	}
}
