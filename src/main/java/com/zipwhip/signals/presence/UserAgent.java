package com.zipwhip.signals.presence;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 6/28/11
 * Time: 3:39 PM
 *
 * Formalizes what information the client can send when they connect.
 * We need this information for sending updates, and knowledge of
 * where our shit is installed.
 */
public class UserAgent implements Serializable {

	private static final long serialVersionUID = -3759346394112106244L;
	/**
	 * The make/model of the device, in the case of a browser it will be NULL
	 * 
	 * (hardware)
	 */
	private String makeModel;
	/**
	 * The OS build version
	 * A json string with these key/val pairs
	 * (for example on the phone)
	 * android.os.Build.DEVICE
	 * android.os.Build.HARDWARE
	 * android.os.Build.DISPLAY
	 *
	 * (software)
	 */
	private String build;
	/**
	 * Software on the phone/browser
	 * eg.
	 * ZIPGROUPS, DEVICE_CARBON, TABLET_TEXTER, PEEPS
	 * and version number
	 */
	private Product product;

	public String getMakeModel() {
		return makeModel;
	}

	public void setMakeModel(String makeModel) {
		this.makeModel = makeModel;
	}

	public String getBuild() {
		return build;
	}

	public void setBuild(String build) {
		this.build = build;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((build == null) ? 0 : build.hashCode());
		result = prime * result + ((makeModel == null) ? 0 : makeModel.hashCode());
		result = prime * result + ((product == null) ? 0 : product.hashCode());
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
		UserAgent other = (UserAgent) obj;
		if (build == null) {
			if (other.build != null)
				return false;
		} else if (!build.equals(other.build))
			return false;
		if (makeModel == null) {
			if (other.makeModel != null)
				return false;
		} else if (!makeModel.equals(other.makeModel))
			return false;
		if (product == null) {
			if (other.product != null)
				return false;
		} else if (!product.equals(other.product))
			return false;
		return true;
	}
}
