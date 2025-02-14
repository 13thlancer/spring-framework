/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.service.invoker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Factory to create a client proxy from an HTTP service interface with
 * {@link HttpExchange @HttpExchange} methods.
 *
 * <p>To create an instance, use static methods to obtain a {@link Builder Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 * @see org.springframework.web.reactive.function.client.support.WebClientAdapter
 */
public final class HttpServiceProxyFactory implements InitializingBean, EmbeddedValueResolverAware {

	@Nullable
	private final BuilderInitializedFactory builderInitializedFactory;

	@Nullable
	private final BeanStyleFactory beanStyleFactory;


	/**
	 * Create an instance with the underlying HTTP client to use.
	 * @param clientAdapter an adapter for the client
	 * @deprecated as of 6.0 RC1 in favor of using the Builder to initialize
	 * the HttpServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC1", forRemoval = true)
	public HttpServiceProxyFactory(HttpClientAdapter clientAdapter) {
		this.beanStyleFactory = new BeanStyleFactory(clientAdapter);
		this.builderInitializedFactory = null;
	}

	private HttpServiceProxyFactory(
			HttpClientAdapter clientAdapter, List<HttpServiceArgumentResolver> argumentResolvers,
			@Nullable StringValueResolver embeddedValueResolver,
			ReactiveAdapterRegistry reactiveAdapterRegistry, Duration blockTimeout) {

		this.beanStyleFactory = null;
		this.builderInitializedFactory = new BuilderInitializedFactory(
				clientAdapter, argumentResolvers, embeddedValueResolver, reactiveAdapterRegistry, blockTimeout);
	}


	/**
	 * Register a custom argument resolver, invoked ahead of default resolvers.
	 * @param resolver the resolver to add
	 * @deprecated as of 6.0 RC1 in favor of using the Builder to initialize
	 * the HttpServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC1", forRemoval = true)
	public void addCustomArgumentResolver(HttpServiceArgumentResolver resolver) {
		Assert.state(this.beanStyleFactory != null, "HttpServiceProxyFactory was created through the builder");
		this.beanStyleFactory.addCustomArgumentResolver(resolver);
	}

	/**
	 * Set the custom argument resolvers to use, ahead of default resolvers.
	 * @param resolvers the resolvers to use
	 * @deprecated as of 6.0 RC1 in favor of using the Builder to initialize
	 * the HttpServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC1", forRemoval = true)
	public void setCustomArgumentResolvers(List<HttpServiceArgumentResolver> resolvers) {
		Assert.state(this.beanStyleFactory != null, "HttpServiceProxyFactory was created through the builder");
		this.beanStyleFactory.setCustomArgumentResolvers(resolvers);
	}

	/**
	 * Set the {@link ConversionService} to use where input values need to
	 * be formatted as Strings.
	 * <p>By default this is {@link DefaultFormattingConversionService}.
	 * @deprecated as of 6.0 RC1 in favor of using the Builder to initialize
	 * the HttpServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC1", forRemoval = true)
	public void setConversionService(ConversionService conversionService) {
		Assert.state(this.beanStyleFactory != null, "HttpServiceProxyFactory was created through the builder");
		this.beanStyleFactory.setConversionService(conversionService);
	}

	/**
	 * Set the StringValueResolver to use for resolving placeholders and
	 * expressions in {@link HttpExchange#url()}.
	 * @param resolver the resolver to use
	 * @deprecated as of 6.0 RC1 in favor of using the Builder to initialize
	 * an HttpServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC1", forRemoval = true)
	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		Assert.state(this.beanStyleFactory != null, "HttpServiceProxyFactory was created through the builder");
		this.beanStyleFactory.setEmbeddedValueResolver(resolver);
	}

	/**
	 * Set the {@link ReactiveAdapterRegistry} to use to support different
	 * asynchronous types for HTTP service method return values.
	 * <p>By default this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
	 * @deprecated as of 6.0 RC1 in favor of using the Builder to initialize
	 * an HttpServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC1", forRemoval = true)
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		Assert.state(this.beanStyleFactory != null, "HttpServiceProxyFactory was created through the builder");
		this.beanStyleFactory.setReactiveAdapterRegistry(registry);
	}

	/**
	 * Configure how long to wait for a response for an HTTP service method
	 * with a synchronous (blocking) method signature.
	 * <p>By default this is 5 seconds.
	 * @param blockTimeout the timeout value
	 * @deprecated as of 6.0 RC1 in favor of using the Builder to initialize
	 * an HttpServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC1", forRemoval = true)
	public void setBlockTimeout(Duration blockTimeout) {
		Assert.state(this.beanStyleFactory != null, "HttpServiceProxyFactory was created through the builder");
		this.beanStyleFactory.setBlockTimeout(blockTimeout);
	}


	@Override
	@Deprecated
	public void afterPropertiesSet() throws Exception {
		if (this.beanStyleFactory != null) {
			this.beanStyleFactory.afterPropertiesSet();
		}
	}


	/**
	 * Return a proxy that implements the given HTTP service interface to perform
	 * HTTP requests and retrieve responses through an HTTP client.
	 * @param serviceType the HTTP service to create a proxy for
	 * @param <S> the HTTP service type
	 * @return the created proxy
	 */
	public <S> S createClient(Class<S> serviceType) {
		if (this.builderInitializedFactory != null) {
			return this.builderInitializedFactory.createClient(serviceType);
		}
		else if (this.beanStyleFactory != null) {
			return this.beanStyleFactory.createClient(serviceType);
		}
		else {
			throw new IllegalStateException("Expected Builder initialized or Bean-style delegate");
		}
	}


	/**
	 * Return an {@link HttpServiceProxyFactory} builder, initialized with the
	 * given client.
	 */
	public static Builder builder(HttpClientAdapter clientAdapter) {
		return new Builder().clientAdapter(clientAdapter);
	}

	/**
	 * Return an {@link HttpServiceProxyFactory} builder.
	 */
	public static Builder builder() {
		return new Builder();
	}


	/**
	 * Builder to create an {@link HttpServiceProxyFactory}.
	 */
	public static final class Builder {

		@Nullable
		private HttpClientAdapter clientAdapter;

		@Nullable
		private List<HttpServiceArgumentResolver> argumentResolvers;

		@Nullable
		private ConversionService conversionService;

		@Nullable
		private StringValueResolver embeddedValueResolver;

		private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

		@Nullable
		private Duration blockTimeout;

		private Builder() {
		}

		/**
		 * Provide the HTTP client to perform requests through.
		 * @param clientAdapter a client adapted to {@link HttpClientAdapter}
		 * @return this same builder instance
		 */
		public Builder clientAdapter(HttpClientAdapter clientAdapter) {
			this.clientAdapter = clientAdapter;
			return this;
		}

		/**
		 * Register a custom argument resolver, invoked ahead of default resolvers.
		 * @param resolver the resolver to add
		 * @return this same builder instance
		 */
		public Builder customArgumentResolver(HttpServiceArgumentResolver resolver) {
			this.argumentResolvers = (this.argumentResolvers != null ? this.argumentResolvers : new ArrayList<>());
			this.argumentResolvers.add(resolver);
			return this;
		}

		/**
		 * Set the {@link ConversionService} to use where input values need to
		 * be formatted as Strings.
		 * <p>By default this is {@link DefaultFormattingConversionService}.
		 * @return this same builder instance
		 */
		public Builder conversionService(ConversionService conversionService) {
			this.conversionService = conversionService;
			return this;
		}

		/**
		 * Set the {@link StringValueResolver} to use for resolving placeholders
		 * and expressions embedded in {@link HttpExchange#url()}.
		 * @param embeddedValueResolver the resolver to use
		 * @return this same builder instance
		 */
		public Builder embeddedValueResolver(StringValueResolver embeddedValueResolver) {
			this.embeddedValueResolver = embeddedValueResolver;
			return this;
		}

		/**
		 * Set the {@link ReactiveAdapterRegistry} to use to support different
		 * asynchronous types for HTTP service method return values.
		 * <p>By default this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
		 * @return this same builder instance
		 */
		public Builder reactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
			this.reactiveAdapterRegistry = registry;
			return this;
		}

		/**
		 * Configure how long to wait for a response for an HTTP service method
		 * with a synchronous (blocking) method signature.
		 * <p>By default this is 5 seconds.
		 * @param blockTimeout the timeout value
		 * @return this same builder instance
		 */
		public Builder blockTimeout(Duration blockTimeout) {
			this.blockTimeout = blockTimeout;
			return this;
		}

		/**
		 * Build the {@link HttpServiceProxyFactory} instance.
		 */
		public HttpServiceProxyFactory build() {
			Assert.notNull(this.clientAdapter, "HttpClientAdapter is required");

			return new HttpServiceProxyFactory(
					this.clientAdapter, initArgumentResolvers(),
					this.embeddedValueResolver, this.reactiveAdapterRegistry,
					(this.blockTimeout != null ? this.blockTimeout : Duration.ofSeconds(5)));
		}

		private List<HttpServiceArgumentResolver> initArgumentResolvers() {
			List<HttpServiceArgumentResolver> resolvers = new ArrayList<>();

			// Custom
			if (this.argumentResolvers != null) {
				resolvers.addAll(this.argumentResolvers);
			}

			ConversionService service = (this.conversionService != null ?
					this.conversionService : new DefaultFormattingConversionService());

			// Annotation-based
			resolvers.add(new RequestHeaderArgumentResolver(service));
			resolvers.add(new RequestBodyArgumentResolver(this.reactiveAdapterRegistry));
			resolvers.add(new PathVariableArgumentResolver(service));
			resolvers.add(new RequestParamArgumentResolver(service));
			resolvers.add(new CookieValueArgumentResolver(service));
			resolvers.add(new RequestAttributeArgumentResolver());

			// Specific type
			resolvers.add(new UrlArgumentResolver());
			resolvers.add(new HttpMethodArgumentResolver());

			return resolvers;
		}

	}


	/**
	 * {@link MethodInterceptor} that invokes an {@link HttpServiceMethod}.
	 */
	private static final class HttpServiceMethodInterceptor implements MethodInterceptor {

		private final Map<Method, HttpServiceMethod> httpServiceMethods;

		private HttpServiceMethodInterceptor(List<HttpServiceMethod> methods) {
			this.httpServiceMethods = methods.stream()
					.collect(Collectors.toMap(HttpServiceMethod::getMethod, Function.identity()));
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Method method = invocation.getMethod();
			HttpServiceMethod httpServiceMethod = this.httpServiceMethods.get(method);
			if (httpServiceMethod != null) {
				return httpServiceMethod.invoke(invocation.getArguments());
			}
			if (method.isDefault()) {
				if (invocation instanceof ReflectiveMethodInvocation reflectiveMethodInvocation) {
					Object proxy = reflectiveMethodInvocation.getProxy();
					return InvocationHandler.invokeDefault(proxy, method, invocation.getArguments());
				}
			}
			throw new IllegalStateException("Unexpected method invocation: " + method);
		}
	}


	/**
	 * Temporary class until bean-style initialization is removed.
	 */
	private static final class BuilderInitializedFactory {

		private final HttpClientAdapter clientAdapter;

		private final List<HttpServiceArgumentResolver> argumentResolvers;

		@Nullable
		private final StringValueResolver embeddedValueResolver;

		private final ReactiveAdapterRegistry reactiveAdapterRegistry;

		private final Duration blockTimeout;

		private BuilderInitializedFactory(
				HttpClientAdapter clientAdapter, List<HttpServiceArgumentResolver> argumentResolvers,
				@Nullable StringValueResolver embeddedValueResolver,
				ReactiveAdapterRegistry reactiveAdapterRegistry, Duration blockTimeout) {

			this.clientAdapter = clientAdapter;
			this.argumentResolvers = argumentResolvers;
			this.embeddedValueResolver = embeddedValueResolver;
			this.reactiveAdapterRegistry = reactiveAdapterRegistry;
			this.blockTimeout = blockTimeout;
		}

		public <S> S createClient(Class<S> serviceType) {

			List<HttpServiceMethod> httpServiceMethods =
					MethodIntrospector.selectMethods(serviceType, this::isExchangeMethod).stream()
							.map(method -> createHttpServiceMethod(serviceType, method))
							.toList();

			return ProxyFactory.getProxy(serviceType, new HttpServiceMethodInterceptor(httpServiceMethods));
		}

		private boolean isExchangeMethod(Method method) {
			return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class);
		}

		private <S> HttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
			Assert.notNull(this.argumentResolvers,
					"No argument resolvers: afterPropertiesSet was not called");

			return new HttpServiceMethod(
					method, serviceType, this.argumentResolvers, this.clientAdapter,
					this.embeddedValueResolver, this.reactiveAdapterRegistry, this.blockTimeout);
		}

	}


	/**
	 * Temporary class to support bean-style initialization during deprecation period.
	 */
	private static final class BeanStyleFactory implements InitializingBean, EmbeddedValueResolverAware {

		private final HttpClientAdapter clientAdapter;

		@Nullable
		private List<HttpServiceArgumentResolver> customArgumentResolvers;

		@Nullable
		private List<HttpServiceArgumentResolver> argumentResolvers;

		@Nullable
		private ConversionService conversionService;

		@Nullable
		private StringValueResolver embeddedValueResolver;

		private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

		private Duration blockTimeout = Duration.ofSeconds(5);

		BeanStyleFactory(HttpClientAdapter clientAdapter) {
			Assert.notNull(clientAdapter, "HttpClientAdapter is required");
			this.clientAdapter = clientAdapter;
		}

		public void addCustomArgumentResolver(HttpServiceArgumentResolver resolver) {
			if (this.customArgumentResolvers == null) {
				this.customArgumentResolvers = new ArrayList<>();
			}
			this.customArgumentResolvers.add(resolver);
		}

		public void setCustomArgumentResolvers(List<HttpServiceArgumentResolver> resolvers) {
			this.customArgumentResolvers = new ArrayList<>(resolvers);
		}

		public void setConversionService(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		@Override
		public void setEmbeddedValueResolver(StringValueResolver resolver) {
			this.embeddedValueResolver = resolver;
		}

		public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
			this.reactiveAdapterRegistry = registry;
		}

		public void setBlockTimeout(Duration blockTimeout) {
			this.blockTimeout = blockTimeout;
		}


		@Override
		public void afterPropertiesSet() throws Exception {

			this.conversionService = (this.conversionService != null ?
					this.conversionService : new DefaultFormattingConversionService());

			this.argumentResolvers = initArgumentResolvers(this.conversionService);
		}

		private List<HttpServiceArgumentResolver> initArgumentResolvers(ConversionService conversionService) {
			List<HttpServiceArgumentResolver> resolvers = new ArrayList<>();

			// Custom
			if (this.customArgumentResolvers != null) {
				resolvers.addAll(this.customArgumentResolvers);
			}

			// Annotation-based
			resolvers.add(new RequestHeaderArgumentResolver(conversionService));
			resolvers.add(new RequestBodyArgumentResolver(this.reactiveAdapterRegistry));
			resolvers.add(new PathVariableArgumentResolver(conversionService));
			resolvers.add(new RequestParamArgumentResolver(conversionService));
			resolvers.add(new CookieValueArgumentResolver(conversionService));
			resolvers.add(new RequestAttributeArgumentResolver());

			// Specific type
			resolvers.add(new UrlArgumentResolver());
			resolvers.add(new HttpMethodArgumentResolver());

			return resolvers;
		}


		public <S> S createClient(Class<S> serviceType) {

			List<HttpServiceMethod> httpServiceMethods =
					MethodIntrospector.selectMethods(serviceType, this::isExchangeMethod).stream()
							.map(method -> createHttpServiceMethod(serviceType, method))
							.toList();

			return ProxyFactory.getProxy(serviceType, new HttpServiceMethodInterceptor(httpServiceMethods));
		}

		private boolean isExchangeMethod(Method method) {
			return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class);
		}

		private <S> HttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
			Assert.notNull(this.argumentResolvers,
					"No argument resolvers: afterPropertiesSet was not called");

			return new HttpServiceMethod(
					method, serviceType, this.argumentResolvers, this.clientAdapter,
					this.embeddedValueResolver, this.reactiveAdapterRegistry, this.blockTimeout);
		}

	}

}
