import { useEffect, useCallback } from 'react';

interface HelpSection {
  title: string;
  content: string | React.ReactNode;
}

interface HelpModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  sections: HelpSection[];
}

export default function HelpModal({ isOpen, onClose, title, sections }: HelpModalProps) {
  // Close on Escape key
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      onClose();
    }
  }, [onClose]);

  useEffect(() => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen, handleKeyDown]);

  if (!isOpen) return null;

  return (
    <div className="help-modal-overlay" onClick={onClose}>
      <div className="help-modal" onClick={e => e.stopPropagation()}>
        <div className="help-modal-header">
          <h2>{title}</h2>
          <button className="help-modal-close" onClick={onClose} title="Close (Esc)">
            &times;
          </button>
        </div>
        <div className="help-modal-content">
          {sections.map((section, index) => (
            <div key={index} className="help-section">
              <h3>{section.title}</h3>
              <div className="help-section-content">
                {typeof section.content === 'string' ? (
                  <p>{section.content}</p>
                ) : (
                  section.content
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// Help button component for consistent styling
interface HelpButtonProps {
  onClick: () => void;
}

export function HelpButton({ onClick }: HelpButtonProps) {
  return (
    <button className="help-button" onClick={onClick} title="Help &amp; Information">
      ?
    </button>
  );
}
