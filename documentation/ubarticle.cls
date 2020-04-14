\LoadClass{article}
\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{ubarticle}[2019/06/03 Ubique Corporate Design]

\RequirePackage{titlesec}
\RequirePackage{fontspec}
\RequirePackage{array}
\RequirePackage[table,svgnames]{xcolor}
\RequirePackage{tabularx}
\RequirePackage{etoolbox}
\RequirePackage{cellspace}
\RequirePackage{makecell}
\RequirePackage{fancyhdr}
\RequirePackage{lastpage}
\RequirePackage[a4paper,left=25mm,top=30mm, right=25mm,bottom=30mm]{geometry}
\setmainfont{Lato}[Ligatures=TeX,Extension = .ttf, Path = ./Lato/, UprightFont = *-Regular,
BoldFont = *-Bold]


\titleformat{\section}         % Customise the \section command 
{\Large\scshape\raggedright} % Make the \section headers large (\Large),
% small capitals (\scshape) and left aligned (\raggedright)
{}{0em}                      % Can be used to give a prefix to all sections, like 'Section ...'
{}                           % Can be used to insert code before the heading            

\titleformat{\subsection}
{\large\scshape\raggedright}
{}{0em}
{}
\titleformat{\subsubsection}
{\color{gray}\large\scshape\raggedright}
{}{0em}
{}

%vertical middle alignments
\renewcommand\tabularxcolumn[1]{m{#1}}
\AtBeginEnvironment{verbatim}{\color{ublau}}

\newcommand{\ubheader}[1]{\textcolor{ublau}{\textbf{#1}}}
\definecolor{ublau}{HTML}{387CF7}
\definecolor{ubgray}{HTML}{F0F0F0}
\newcolumntype{Y}{>{\centering\arraybackslash}X}
\newcolumntype{H}{>{ \columncolor{ubgray}\centering\arraybackslash\color{ublau}}c}

\renewcommand{\arraystretch}{1.5}

\newenvironment{ubparam}[2]{\tabularx{#1}{#2}	\hline
	\rowcolor{ubgray}} {\endtabularx}
\newenvironment{ubresponses}[2]{	\tabularx{#1}{#2}	\hline
		\rowcolor{ubgray}}{\endtabularx}

\pagestyle{fancy}
\fancyhf{}
\rfoot{\textcolor{gray}{\today}}
\cfoot{\textcolor{gray}{\thepage~von~\pageref{LastPage} }}
\lfoot{\textcolor{gray}{Vertraulich}}


