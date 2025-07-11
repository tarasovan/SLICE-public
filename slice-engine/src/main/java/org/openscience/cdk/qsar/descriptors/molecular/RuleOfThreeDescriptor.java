/*  Copyright (C) 2004-2007  The Chemistry Development Kit (CDK) project
 *
 *  Contact: cdk-devel@lists.sourceforge.net
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.qsar.descriptors.molecular;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerResult;

/**
 * This Class contains a method that returns the number failures of the
 * Lipinski's Rule Of 5.
 * See <a href="http://en.wikipedia.org/wiki/Lipinski%27s_Rule_of_Five">http://en.wikipedia.org/wiki/Lipinski%27s_Rule_of_Five</a>.
 *
  * <table border="1"><caption>Parameters for this descriptor:</caption>
 *   <tr>
 *     <td>Name</td>
 *     <td>Default</td>
 *     <td>Description</td>
 *   </tr>
 *   <tr>
 *     <td>checkAromaticity</td>
 *     <td>false</td>
 *     <td>True is the aromaticity has to be checked</td>
 *   </tr>
 * </table>
 *
 * Returns a single value named <i>LipinskiFailures</i>.
 *
 * @author      mfe4
 * @cdk.created 2004-11-03
 * @cdk.module  qsarmolecular
 * @cdk.githash
 * @cdk.dictref qsar-descriptors:lipinskifailures
 *
 * @cdk.keyword Lipinski
 * @cdk.keyword rule-of-three
 * @cdk.keyword descriptor
 *  */
public class RuleOfThreeDescriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {

    private boolean               checkAromaticity = false;

    private static final String[] NAMES            = {"LipinskiFailures"};

    /**
     *  Constructor for the RuleOfFiveDescriptor object.
     */
    public RuleOfThreeDescriptor() {}

    /**
     * Returns a <code>Map</code> which specifies which descriptor
     * is implemented by this class.
     *
     * These fields are used in the map:
     * <ul>
     * <li>Specification-Reference: refers to an entry in a unique dictionary
     * <li>Implementation-Title: anything
     * <li>Implementation-Identifier: a unique identifier for this version of
     *  this class
     * <li>Implementation-Vendor: CDK, JOELib, or anything else
     * </ul>
     *
     * @return An object containing the descriptor specification
     */
    @Override
    public DescriptorSpecification getSpecification() {
        return new DescriptorSpecification(
                "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#lipinskifailures", this.getClass()
                        .getName(), "The Chemistry Development Kit");
    }

    /**
     *  Sets the parameters attribute of the RuleOfFiveDescriptor object.
         *
         *  There is only one parameter, which should be a Boolean indicating whether
         *  aromaticity should be checked or has already been checked. The name of the paramete
         *  is checkAromaticity.
     *
     *@param  params            Parameter is only one: a boolean.
     *@throws  CDKException  if more than 1 parameter or a non-Boolean parameter is specified
         *@see #getParameters
     */
    @Override
    public void setParameters(Object[] params) throws CDKException {
        if (params.length != 1) {
            throw new CDKException("RuleOfThreeDescriptor expects one parameter");
        }
        if (!(params[0] instanceof Boolean)) {
            throw new CDKException("The first parameter must be of type Boolean");
        }
        // ok, all should be fine
        checkAromaticity = (Boolean) params[0];
    }

    /**
     *  Gets the parameters attribute of the RuleOfFiveDescriptor object.
     *
     *@return    The parameters value
         *@see #setParameters
     */
    @Override
    public Object[] getParameters() {
        // return the parameters as used for the descriptor calculation
        Object[] params = new Object[1];
        params[0] = checkAromaticity;
        return params;
    }

    @Override
    public String[] getDescriptorNames() {
        return NAMES;
    }

    /**
     *  the method take a boolean checkAromaticity: if the boolean is true, it means that
     *  aromaticity has to be checked.
     *
     *@param  mol   AtomContainer for which this descriptor is to be calculated
     *@return    The number of failures of the Lipinski rule
     */
    @Override
    public DescriptorValue calculate(IAtomContainer mol) {
        mol = clone(mol); // don't mod original
        int lipinskifailures = 0;

        IMolecularDescriptor xlogP = new XLogPDescriptor();
        Object[] xlogPparams = {checkAromaticity, Boolean.TRUE,};

        try {
            xlogP.setParameters(xlogPparams);
            double xlogPvalue = ((DoubleResult) xlogP.calculate(mol).getValue()).doubleValue();

            IMolecularDescriptor acc = new HBondAcceptorCountDescriptor();
            Object[] hBondparams = {checkAromaticity};
            acc.setParameters(hBondparams);
            int acceptors = ((IntegerResult) acc.calculate(mol).getValue()).intValue();

            IMolecularDescriptor don = new HBondDonorCountDescriptor();
            don.setParameters(hBondparams);
            int donors = ((IntegerResult) don.calculate(mol).getValue()).intValue();

            IMolecularDescriptor mw = new WeightDescriptor();
            Object[] mwparams = {"*"};
            mw.setParameters(mwparams);
            double mwvalue = ((DoubleResult) mw.calculate(mol).getValue()).doubleValue();

            // exclude (heavy atom) terminal bonds
            // exclude amide C-N bonds because of their high rotational barrier
            // see Veber, D.F. et al., 2002, 45(12), pp.2615–23.
            IMolecularDescriptor rotata = new RotatableBondsCountDescriptor();
            Object[] rotatableBondsParams = {false, true};
            rotata.setParameters(rotatableBondsParams);
            int rotatablebonds = ((IntegerResult) rotata.calculate(mol).getValue()).intValue();

            if (xlogPvalue > 3.0) {
                lipinskifailures += 1;
            }
            if (acceptors > 3) {
                lipinskifailures += 1;
            }
            if (donors > 3) {
                lipinskifailures += 1;
            }
            if (mwvalue > 300.0) {
                lipinskifailures += 1;
            }
            if (rotatablebonds > 3.0) {
                lipinskifailures += 1;
            }
        } catch (CDKException e) {
            new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new IntegerResult(
                    (int) Double.NaN), getDescriptorNames(), e);
        }

        return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new IntegerResult(
                lipinskifailures), getDescriptorNames());
    }

    /**
     * Returns the specific type of the DescriptorResult object.
     * 
     * The return value from this method really indicates what type of result will
     * be obtained from the {@link org.openscience.cdk.qsar.DescriptorValue} object. Note that the same result
     * can be achieved by interrogating the {@link org.openscience.cdk.qsar.DescriptorValue} object; this method
     * allows you to do the same thing, without actually calculating the descriptor.
     *
     * @return an object that implements the {@link org.openscience.cdk.qsar.result.IDescriptorResult} interface indicating
     *         the actual type of values returned by the descriptor in the {@link org.openscience.cdk.qsar.DescriptorValue} object
     */
    @Override
    public IDescriptorResult getDescriptorResultType() {
        return new IntegerResult(1);
    }

    /**
     *  Gets the parameterNames attribute of the RuleOfFiveDescriptor object.
     *
     *@return    The parameterNames value
     */
    @Override
    public String[] getParameterNames() {
        String[] params = new String[1];
        params[0] = "checkAromaticity";
        return params;
    }

    /**
     *  Gets the parameterType attribute of the RuleOfFiveDescriptor object.
     *
     *@param  name  The name of the parameter. In this case it is 'checkAromaticity'.
     *@return       An Object of class equal to that of the parameter being requested
     */
    @Override
    public Object getParameterType(String name) {
        return true;
    }
}
