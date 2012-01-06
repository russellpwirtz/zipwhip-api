package com.zipwhip.signals.presence;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 6/28/11
 * Time: 3:40 PM
 */
public class Product implements Serializable {

	private static final long serialVersionUID = -2735403011556045497L;

	/**
	 * This is the friendly name for the software on the phone/browser
	 * e.g.  ZIPGROUPS, DEVICE_CARBON, TABLET_TEXTER, PEEPS
	 */
	private ProductLine name;

	/**
	 * Version of the software
	 */
	private String version;

	/**
	 * Aka the build of the software that will
	 * correlate to a set of packages
	 */
	private String build;

	public ProductLine getName() {
		return name;
	}

	public void setName(ProductLine name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getBuild() {
		return build;
	}

	public void setBuild(String build) {
		this.build = build;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((build == null) ? 0 : build.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Product other = (Product) obj;
		if (build == null) {
			if (other.build != null)
				return false;
		} else if (!build.equals(other.build))
			return false;
		if (name != other.name)
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
}
