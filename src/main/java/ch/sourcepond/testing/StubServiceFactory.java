package ch.sourcepond.testing;

public interface StubServiceFactory<T> {

	/**
	 * @return
	 */
	T create();

	/**
	 * @param pService
	 */
	void destroy(T pService);
}
