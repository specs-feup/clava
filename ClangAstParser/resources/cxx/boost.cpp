
#include <boost/program_options/options_description.hpp>
#include <boost/program_options/parsers.hpp>
#include <boost/program_options/variables_map.hpp>

namespace po = boost::program_options;

/**
 * The decription of the application
 */
po::options_description opts_desc("Some Description");

int main(int argc, char* argv[]) {
	
	std::string pocket_filename;
	
	opts_desc.add_options()
	//("help,h", "print this help message")
	("pocket,p", po::value<std::string>(&pocket_filename)->default_value("pocket.pdb"), "The pocket DB");
/*
	("ligand,l", po::value<std::string>(&ligands_filename)->default_value("ligand.mol2"), "The ligand DB")
	("surface,s", po::value<std::string>(&surface_filename)->default_value("SURFACE_DEF_XTOOL"), "Surface file?")
	("num,m", po::value<int>(&num_ligand_considered)->default_value(1), "Number of ligand considered")
	;
	*/
	return 0;
}